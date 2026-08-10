package com.safefood.setup.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL을 쓰지 않을 때 사용하는 {@code data/} 파일 저장소 준비.
 *
 * <p>{@code docs/SetupWizard.md}의 <b>데이터베이스 미사용 — data/ 폴더</b> 절과 같은 내용입니다.
 * 폴더 구조를 바꿀 때는 <b>이 파일과 문서를 함께</b> 고쳐 주세요.
 *
 * <ul>
 *   <li>{@code data/public/} — 앱이 동작하는 데 필요한 공통 데이터 (커밋 가능)</li>
 *   <li>{@code data/private/} — 개인 데이터 + 팀 서버 데이터 (커밋 금지, .gitignore 등록)</li>
 * </ul>
 *
 * <p><b>이미 있는 파일은 절대 덮어쓰지 않습니다.</b> 그래서 마법사를 여러 번 실행해도
 * 쌓아 둔 회원·그룹 데이터가 사라지지 않습니다. DB를 쓰더라도 폴더는 미리 만들어 두므로,
 * 나중에 MySQL을 끄고 파일 모드로 바꿔도 그대로 쓸 수 있습니다.
 *
 * <p>저장 형식은 UTF-8 텍스트, <b>한 줄 = 한 레코드</b>, 필드 구분자는 {@code |} 입니다.
 * 필드 순서는 {@link Schema}의 테이블 컬럼 순서와 맞춰 두었습니다.
 * (주석 줄을 넣으면 그 줄도 레코드로 읽히므로, 기본 파일에는 주석을 넣지 않습니다.)
 */
final class DataStoreInitializer {

    private static final String PUBLIC_DIR = "public";
    private static final String PRIVATE_DIR = "private";

    /**
     * {@code data/public/} — 앱 공통 데이터.
     * 값이 있는 파일은 기본 목록으로, 나머지는 빈 파일로 만듭니다.
     *
     * <p>필드 순서
     * <ul>
     *   <li>{@code allergy.txt} — name</li>
     *   <li>{@code mood.txt} — name</li>
     *   <li>{@code restaurant.txt} — id|name|category|address|phone|open_time|close_time|latitude|longitude</li>
     *   <li>{@code menu.txt} — id|restaurant_id|name|price|category|spicy_level|description</li>
     * </ul>
     */
    private static final Map<String, List<String>> PUBLIC_FILES = new LinkedHashMap<>();

    /**
     * {@code data/private/} — 개인·팀 서버 데이터. 처음에는 모두 빈 파일입니다.
     *
     * <p>필드 순서
     * <ul>
     *   <li>{@code users.txt} — id|login_id|password|nickname|created_at</li>
     *   <li>{@code preferences.txt} — user_id|spicy_level|price_min|price_max|max_distance</li>
     *   <li>{@code favorites.txt} — id|user_id|restaurant_id|menu_id|created_at</li>
     *   <li>{@code history.txt} — id|user_id|menu_id|group_id|type|created_at</li>
     *   <li>{@code groups.txt} — id|name|owner_id|invite_code|status|created_at</li>
     *   <li>{@code group_members.txt} — id|group_id|user_id|guest_name|latitude|longitude|joined_at</li>
     *   <li>{@code votes.txt} — candidate_id|member_id|voted_at</li>
     * </ul>
     */
    private static final List<String> PRIVATE_FILES = List.of(
            "users.txt",
            "preferences.txt",
            "favorites.txt",
            "history.txt",
            "groups.txt",
            "group_members.txt",
            "votes.txt"
    );

    static {
        PUBLIC_FILES.put("allergy.txt", Schema.ALLERGIES);
        PUBLIC_FILES.put("mood.txt", Schema.MOODS);
        PUBLIC_FILES.put("restaurant.txt", List.of());
        PUBLIC_FILES.put("menu.txt", List.of());
    }

    private final Path projectRoot;
    private final SetupReporter out;

    DataStoreInitializer(Path projectRoot, SetupReporter out) {
        this.projectRoot = projectRoot;
        this.out = out;
    }

    /**
     * {@code data/public/}·{@code data/private/}와 기본 파일을 준비합니다.
     *
     * @return 이번 실행에서 <b>새로 만든</b> 파일 수 (이미 있던 파일은 세지 않음)
     */
    int prepare() throws IOException {
        Path dataDir = projectRoot.resolve("data");
        Path publicDir = dataDir.resolve(PUBLIC_DIR);
        Path privateDir = dataDir.resolve(PRIVATE_DIR);

        Files.createDirectories(publicDir);
        Files.createDirectories(privateDir);

        int created = 0;
        for (Map.Entry<String, List<String>> entry : PUBLIC_FILES.entrySet()) {
            created += createIfAbsent(publicDir.resolve(entry.getKey()), entry.getValue(), PUBLIC_DIR);
        }
        for (String name : PRIVATE_FILES) {
            created += createIfAbsent(privateDir.resolve(name), List.of(), PRIVATE_DIR);
        }
        // .gitignore가 data/private/를 통째로 제외하므로, 폴더 자체를 저장소에 남기려면 이 파일이 필요합니다.
        created += createIfAbsent(privateDir.resolve(".gitkeep"), List.of(), PRIVATE_DIR);

        return created;
    }

    /**
     * 파일이 없을 때만 만듭니다. 이미 있으면 손대지 않습니다.
     *
     * @return 새로 만들었으면 1, 건너뛰었으면 0
     */
    private int createIfAbsent(Path file, List<String> lines, String folderLabel) throws IOException {
        if (Files.exists(file)) {
            return 0;
        }
        String content = lines.isEmpty() ? "" : String.join(System.lineSeparator(), lines) + System.lineSeparator();
        Files.writeString(file, content, StandardCharsets.UTF_8);

        String size = lines.isEmpty() ? "빈 파일" : lines.size() + "건";
        out.detail("+ data/" + folderLabel + "/" + file.getFileName() + " (" + size + ")");
        return 1;
    }
}
