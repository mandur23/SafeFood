package com.safefood.tool;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import com.safefood.dao.DatabaseUtil;
import com.safefood.setup.core.Schema;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class MenuGenImporter {

    private static final String ENDPOINT =
            "https://apis.data.go.kr/1390803/AgriFood/FdFood1/getKoreanFoodFdFoodList1";
    private static final int PAGE_SIZE = 20;

    private static final List<String> DICTIONARY_TABLES =
            List.of("food", "ingredient", "food_ingredient", "ingredient_allergy");
    private static final int BATCH_SIZE = 500;

    private static final Map<String, String> ALLERGY_ALIAS = Map.ofEntries(
            Map.entry("난류", "계란"), Map.entry("알류", "계란"), Map.entry("달걀", "계란"),
            Map.entry("콩", "대두"), Map.entry("소고기", "쇠고기"), Map.entry("밀가루", "밀"),
            Map.entry("굴", "조개류"), Map.entry("전복", "조개류"), Map.entry("홍합", "조개류"),
            Map.entry("조개", "조개류"), Map.entry("아황산", "아황산류"), Map.entry("아황산염", "아황산류"));

    private final Path publicDir = Path.of("data", "public");
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private record FoodRow(int id, String foodCd, String name, String category) {
    }

    private final Map<String, FoodRow> foodsByCd = new LinkedHashMap<>();
    private final Map<String, Integer> ingredientIdByName = new LinkedHashMap<>();
    private final Map<Integer, LinkedHashMap<Integer, String>> linksByFoodId = new LinkedHashMap<>();
    private final TreeSet<long[]> allergyLinks =
            new TreeSet<>(Comparator.<long[]>comparingLong(a -> a[0]).thenComparingLong(a -> a[1]));

    private List<String> allergyMaster;
    private final TreeSet<String> unmappedAllergies = new TreeSet<>();

    public static void main(String[] args) {
        try {
            new MenuGenImporter().run(args);
        } catch (Exception e) {
            System.err.println("[실패] " + e.getMessage());
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        String foodName = argValue(args, "--food");
        String key = resolveKey(argValue(args, "--key"));
        int maxPages = Integer.parseInt(
                argValue(args, "--max-pages") == null ? "0" : argValue(args, "--max-pages"));

        allergyMaster = loadAllergyMaster();
        loadStores();

        int newFoods = 0, updatedFoods = 0, page = 1, fetched = 0, total = Integer.MAX_VALUE;
        while (fetched < total && (maxPages == 0 || page <= maxPages)) {
            Document doc = fetchPage(key, foodName, page);
            total = intText(doc, "total_Count");
            List<Element> items = new ArrayList<>();
            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                items.add((Element) itemNodes.item(i));
            }
            if (items.isEmpty()) {
                break;
            }
            for (Element item : items) {
                boolean isNew = upsertFood(item);
                if (isNew) {
                    newFoods++;
                } else {
                    updatedFoods++;
                }
            }
            fetched += items.size();
            System.out.printf("페이지 %d — %d/%d건%n", page, fetched, total);
            page++;
            Thread.sleep(200);
        }

        saveStores();
        importIntoDatabase();

        System.out.printf("%n[완료] 음식 %d건 (신규 %d · 갱신 %d), 재료 %d종, 재료-알레르기 연결 %d건%n",
                foodsByCd.size(), newFoods, updatedFoods, ingredientIdByName.size(), allergyLinks.size());
        if (!unmappedAllergies.isEmpty()) {
            System.out.println("[경고] allergy.txt 마스터로 매핑하지 못한 알레르기 표기 — 별칭 표에 추가를 검토하세요:");
            unmappedAllergies.forEach(name -> System.out.println("  - " + name));
        }
    }

    private Document fetchPage(String key, String foodName, int page) throws Exception {
        StringBuilder url = new StringBuilder(ENDPOINT)
                .append("?serviceKey=").append(encodeKey(key))
                .append("&page_No=").append(page)
                .append("&Page_Size=").append(PAGE_SIZE)
                .append("&service_Type=xml");
        if (foodName != null) {
            url.append("&food_Name=").append(URLEncoder.encode(foodName, StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        String body;
        try {
            body = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        } catch (IOException first) {
            Thread.sleep(1000);
            body = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        }

        Document doc = parseXml(body);
        String root = doc.getDocumentElement().getTagName();
        if ("OpenAPI_ServiceResponse".equals(root)) {
            throw new IOException("API 오류: " + firstText(doc, "returnAuthMsg", "errMsg", "returnReasonCode"));
        }
        int code = intText(doc, "result_Code");
        if (code != 200) {
            throw new IOException("API 오류: result_Code=" + code + ", " + firstText(doc, "result_Msg"));
        }
        return doc;
    }

    private static String encodeKey(String key) {
        return key.contains("%") ? key : URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    private static Document parseXml(String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(body)));
    }

    private boolean upsertFood(Element item) {
        String foodCd = clean(childText(item, "fd_Code"), 20);
        String name = clean(childText(item, "fd_Nm"), 100);
        if (foodCd == null || name == null) {
            return false;
        }
        String upper = clean(childText(item, "upper_Fd_Grupp_Nm"), 25);
        String sub = clean(childText(item, "fd_Grupp_Nm"), 25);
        String category = clean(upper == null ? sub : sub == null ? upper : upper + " > " + sub, 50);

        FoodRow existing = foodsByCd.get(foodCd);
        boolean isNew = existing == null;
        int foodId = isNew ? nextId(foodsByCd.values().stream().mapToInt(FoodRow::id)) : existing.id();
        foodsByCd.put(foodCd, new FoodRow(foodId, foodCd, name, category));

        LinkedHashMap<Integer, String> links = new LinkedHashMap<>();
        for (Element foodList : elements(item, "food_List")) {
            for (Element food : elements(foodList, "food")) {
                String ingName = clean(childText(food, "food_Nm"), 100);
                if (ingName == null) {
                    continue;
                }
                int ingId = ingredientIdByName.computeIfAbsent(ingName,
                        n -> nextId(ingredientIdByName.values().stream().mapToInt(Integer::intValue)));
                links.putIfAbsent(ingId, weightToQuantity(childText(food, "food_Wgh")));
                for (String allergyName : normalizeAllergies(childText(food, "allrgy_Info"))) {
                    allergyLinks.add(new long[]{ingId, allergyMaster.indexOf(allergyName) + 1});
                }
            }
        }
        linksByFoodId.put(foodId, links);
        return isNew;
    }

    private List<String> normalizeAllergies(String raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        var matcher = java.util.regex.Pattern.compile("\\(([^)]*)\\)").matcher(raw);
        while (matcher.find()) {
            tokens.addAll(List.of(matcher.group(1).split("[,·/]")));
        }
        tokens.addAll(List.of(raw.replaceAll("\\([^)]*\\)", " ").split("[,·/]")));

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            String name = token.replaceAll("(포함|함유|등)$", "").trim();
            if (name.isEmpty()) {
                continue;
            }
            String master = allergyMaster.contains(name) ? name : ALLERGY_ALIAS.get(name);
            if (master != null && allergyMaster.contains(master)) {
                if (!result.contains(master)) {
                    result.add(master);
                }
            } else {
                unmappedAllergies.add(name);
            }
        }
        return result;
    }

    private static String weightToQuantity(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            return new BigDecimal(raw).stripTrailingZeros().toPlainString() + "g";
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private List<String> loadAllergyMaster() throws IOException {
        Path file = publicDir.resolve("allergy.txt");
        if (!Files.exists(file)) {
            throw new IOException("data/public/allergy.txt가 없습니다. 먼저 Setup Wizard를 실행하세요.");
        }
        List<String> names = readLines(file);
        if (names.isEmpty()) {
            throw new IOException("data/public/allergy.txt가 비어 있습니다. 먼저 Setup Wizard를 실행하세요.");
        }
        return names;
    }

    private void loadStores() throws IOException {
        for (String[] f : rows("food.txt", 4)) {
            foodsByCd.put(f[1], new FoodRow(Integer.parseInt(f[0]), f[1], f[2], f[3].isEmpty() ? null : f[3]));
        }
        for (String[] f : rows("ingredient.txt", 2)) {
            ingredientIdByName.put(f[1], Integer.parseInt(f[0]));
        }
        for (String[] f : rows("food_ingredient.txt", 3)) {
            linksByFoodId.computeIfAbsent(Integer.parseInt(f[0]), k -> new LinkedHashMap<>())
                    .put(Integer.parseInt(f[1]), f[2]);
        }
        for (String[] f : rows("ingredient_allergy.txt", 2)) {
            allergyLinks.add(new long[]{Long.parseLong(f[0]), Long.parseLong(f[1])});
        }
    }

    private void saveStores() throws IOException {
        Files.createDirectories(publicDir);

        List<String> foodLines = foodsByCd.values().stream()
                .sorted(Comparator.comparingInt(FoodRow::id))
                .map(f -> f.id() + "|" + f.foodCd() + "|" + f.name() + "|" + (f.category() == null ? "" : f.category()))
                .toList();
        writeLines("food.txt", foodLines);

        List<String> ingredientLines = ingredientIdByName.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(e -> e.getValue() + "|" + e.getKey())
                .toList();
        writeLines("ingredient.txt", ingredientLines);

        List<String> linkLines = new ArrayList<>();
        linksByFoodId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> e.getValue().forEach(
                        (ingId, qty) -> linkLines.add(e.getKey() + "|" + ingId + "|" + qty)));
        writeLines("food_ingredient.txt", linkLines);

        writeLines("ingredient_allergy.txt",
                allergyLinks.stream().map(a -> a[0] + "|" + a[1]).toList());
    }

    private void importIntoDatabase() {
        if (!hasDbConfig()) {
            System.out.println("config.properties에 db.url이 없어 DB 반영은 건너뜁니다.");
            return;
        }
        try (Connection con = DatabaseUtil.getConnection()) {
            Map<String, Integer> allergyIdByName;
            try {
                allergyIdByName = selectIdMap(con, "SELECT name, id FROM allergy");
            } catch (SQLException e) {
                allergyIdByName = Map.of();
            }
            if (allergyIdByName.isEmpty()) {
                System.out.println("[경고] DB에 allergy 테이블(기본 데이터)이 없습니다. "
                        + "Setup Wizard로 테이블을 먼저 만드세요. DB 반영은 건너뜁니다.");
                return;
            }

            try (Statement statement = con.createStatement()) {
                for (Schema.Table table : Schema.TABLES) {
                    if (DICTIONARY_TABLES.contains(table.name())) {
                        statement.execute(table.ddl());
                    }
                }
            }

            Map<Integer, String> foodCdByFileId = new LinkedHashMap<>();
            foodsByCd.values().forEach(f -> foodCdByFileId.put(f.id(), f.foodCd()));
            Map<Integer, String> ingredientNameByFileId = new LinkedHashMap<>();
            ingredientIdByName.forEach((name, id) -> ingredientNameByFileId.put(id, name));

            con.setAutoCommit(false);
            upsertFoodsToDb(con);
            upsertIngredientsToDb(con);
            Map<String, Integer> dbFoodIdByCd = selectIdMap(con, "SELECT food_cd, id FROM food");
            Map<String, Integer> dbIngredientIdByName = selectIdMap(con, "SELECT name, id FROM ingredient");
            int linkCount = replaceFoodIngredientsInDb(con, foodCdByFileId, ingredientNameByFileId,
                    dbFoodIdByCd, dbIngredientIdByName);
            int allergyLinkCount = upsertAllergyLinksToDb(con, ingredientNameByFileId,
                    dbIngredientIdByName, allergyIdByName);
            con.commit();

            System.out.printf("[DB] MySQL 반영 완료 — 음식 %d건, 재료 %d종, 재료 구성 %d건, 알레르기 연결 %d건%n",
                    foodsByCd.size(), ingredientIdByName.size(), linkCount, allergyLinkCount);
        } catch (SQLException e) {
            System.err.println("[경고] DB 반영에 실패했습니다 (data/public 파일은 갱신됨): " + e.getMessage());
        }
    }

    private static boolean hasDbConfig() {
        Path config = Path.of("config.properties");
        if (!Files.exists(config)) {
            return false;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(config)) {
            props.load(in);
        } catch (IOException e) {
            return false;
        }
        String url = props.getProperty("db.url");
        return url != null && !url.isBlank();
    }

    private static Map<String, Integer> selectIdMap(Connection con, String sql) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Statement statement = con.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getInt(2));
            }
        }
        return result;
    }

    private void upsertFoodsToDb(Connection con) throws SQLException {
        String sql = "INSERT INTO food (food_cd, name, category) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), category = VALUES(category)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int pending = 0;
            for (FoodRow food : foodsByCd.values()) {
                ps.setString(1, food.foodCd());
                ps.setString(2, food.name());
                ps.setString(3, food.category());
                ps.addBatch();
                if (++pending % BATCH_SIZE == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void upsertIngredientsToDb(Connection con) throws SQLException {
        String sql = "INSERT INTO ingredient (name) VALUES (?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int pending = 0;
            for (String name : ingredientIdByName.keySet()) {
                ps.setString(1, name);
                ps.addBatch();
                if (++pending % BATCH_SIZE == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private int replaceFoodIngredientsInDb(Connection con,
                                           Map<Integer, String> foodCdByFileId,
                                           Map<Integer, String> ingredientNameByFileId,
                                           Map<String, Integer> dbFoodIdByCd,
                                           Map<String, Integer> dbIngredientIdByName) throws SQLException {
        try (PreparedStatement delete = con.prepareStatement("DELETE FROM food_ingredient WHERE food_id = ?")) {
            int pending = 0;
            for (Integer fileFoodId : linksByFoodId.keySet()) {
                Integer dbFoodId = dbFoodIdByCd.get(foodCdByFileId.get(fileFoodId));
                if (dbFoodId == null) {
                    continue;
                }
                delete.setInt(1, dbFoodId);
                delete.addBatch();
                if (++pending % BATCH_SIZE == 0) {
                    delete.executeBatch();
                }
            }
            delete.executeBatch();
        }

        int inserted = 0;
        String sql = "INSERT INTO food_ingredient (food_id, ingredient_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement insert = con.prepareStatement(sql)) {
            int pending = 0;
            for (Map.Entry<Integer, LinkedHashMap<Integer, String>> entry : linksByFoodId.entrySet()) {
                Integer dbFoodId = dbFoodIdByCd.get(foodCdByFileId.get(entry.getKey()));
                if (dbFoodId == null) {
                    continue;
                }
                for (Map.Entry<Integer, String> link : entry.getValue().entrySet()) {
                    Integer dbIngredientId = dbIngredientIdByName.get(ingredientNameByFileId.get(link.getKey()));
                    if (dbIngredientId == null) {
                        continue;
                    }
                    insert.setInt(1, dbFoodId);
                    insert.setInt(2, dbIngredientId);
                    insert.setString(3, link.getValue());
                    insert.addBatch();
                    inserted++;
                    if (++pending % BATCH_SIZE == 0) {
                        insert.executeBatch();
                    }
                }
            }
            insert.executeBatch();
        }
        return inserted;
    }

    private int upsertAllergyLinksToDb(Connection con,
                                       Map<Integer, String> ingredientNameByFileId,
                                       Map<String, Integer> dbIngredientIdByName,
                                       Map<String, Integer> allergyIdByName) throws SQLException {
        int count = 0;
        String sql = "INSERT INTO ingredient_allergy (ingredient_id, allergy_id) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE allergy_id = VALUES(allergy_id)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (long[] link : allergyLinks) {
                String allergyName = link[1] >= 1 && link[1] <= allergyMaster.size()
                        ? allergyMaster.get((int) link[1] - 1) : null;
                Integer dbIngredientId = dbIngredientIdByName.get(ingredientNameByFileId.get((int) link[0]));
                Integer dbAllergyId = allergyName == null ? null : allergyIdByName.get(allergyName);
                if (dbIngredientId == null || dbAllergyId == null) {
                    continue;
                }
                ps.setInt(1, dbIngredientId);
                ps.setInt(2, dbAllergyId);
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private List<String[]> rows(String fileName, int fieldCount) throws IOException {
        List<String[]> result = new ArrayList<>();
        Path file = publicDir.resolve(fileName);
        if (!Files.exists(file)) {
            return result;
        }
        for (String line : readLines(file)) {
            String[] fields = line.split("\\|", -1);
            if (fields.length >= fieldCount) {
                result.add(fields);
            }
        }
        return result;
    }

    private static List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private void writeLines(String fileName, List<String> lines) throws IOException {
        String content = lines.isEmpty() ? "" : String.join(System.lineSeparator(), lines) + System.lineSeparator();
        Files.writeString(publicDir.resolve(fileName), content, StandardCharsets.UTF_8);
        System.out.println("+ data/public/" + fileName + " (" + lines.size() + "건)");
    }

    private static int nextId(java.util.stream.IntStream usedIds) {
        return usedIds.max().orElse(0) + 1;
    }

    private static String clean(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('|', ' ').replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty() || "null".equalsIgnoreCase(cleaned)) {
            return null;
        }
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    private static String argValue(String[] args, String name) {
        for (String arg : args) {
            if (arg.startsWith(name + "=")) {
                return arg.substring(name.length() + 1);
            }
        }
        return null;
    }

    private static String resolveKey(String argKey) throws IOException {
        if (argKey != null && !argKey.isBlank()) {
            return argKey;
        }
        String envKey = System.getenv("MENUGEN_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        Path config = Path.of("config.properties");
        if (Files.exists(config)) {
            Properties props = new Properties();
            try (var in = Files.newInputStream(config)) {
                props.load(in);
            }
            String configKey = props.getProperty("menugen.api.key");
            if (configKey != null && !configKey.isBlank()) {
                return configKey;
            }
        }
        throw new IOException("서비스 키가 없습니다. --key= 인자, 환경 변수 MENUGEN_API_KEY, "
                + "config.properties의 menugen.api.key 중 하나로 알려 주세요.");
    }

    private static String childText(Element parent, String tag) {
        for (Element child : elements(parent, tag)) {
            String text = child.getTextContent();
            return text == null || "null".equalsIgnoreCase(text.trim()) ? null : text.trim();
        }
        return null;
    }

    private static List<Element> elements(Element parent, String tag) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && tag.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static int intText(Document doc, String tag) {
        String text = firstText(doc, tag);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String firstText(Document doc, String... tags) {
        for (String tag : tags) {
            NodeList nodes = doc.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                String text = nodes.item(0).getTextContent();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return "?";
    }
}
