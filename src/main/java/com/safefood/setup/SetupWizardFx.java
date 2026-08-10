package com.safefood.setup;

import com.safefood.setup.core.ConfigFileWriter;
import com.safefood.setup.core.DatabaseInitializer;
import com.safefood.setup.core.DbConfig;
import com.safefood.setup.core.JdbcDriverSetup;
import com.safefood.setup.core.SetupReporter;
import com.safefood.setup.core.SetupRequest;
import com.safefood.setup.core.SetupResult;
import com.safefood.setup.core.SetupService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SafeFood 개발 환경 설정 마법사 — <b>창(JavaFX) 화면</b>.
 *
 * <p>{@link SetupWizard}(콘솔)와 하는 일이 완전히 같습니다. 값을 모아
 * {@link SetupRequest}를 만들고 {@link SetupService}에 넘기는 구조라서,
 * 어느 쪽으로 실행해도 결과가 같습니다. 이 클래스에는 <b>화면 그리는 코드만</b> 있습니다.
 *
 * <p>콘솔판보다 나아진 점
 * <ul>
 *   <li>왼쪽에 단계 목록이 계속 보여서 지금 어디쯤인지 알 수 있습니다.</li>
 *   <li>입력값을 <b>되돌아가서 고칠 수</b> 있습니다. (콘솔은 한 번 지나가면 끝)</li>
 *   <li>포트·DB 이름을 잘못 넣으면 그 자리에서 빨간 글씨로 알려 줍니다.</li>
 *   <li>연결 테스트와 실제 설치를 <b>백그라운드 스레드</b>에서 돌려 창이 멈추지 않습니다.</li>
 *   <li>비밀번호가 화면에 그대로 보이는 문제가 없습니다. (콘솔은 IntelliJ에서 가릴 수 없음)</li>
 * </ul>
 *
 * <p>실행: {@code mvn javafx:run -Pwizard-fx}
 *
 * <p><b>IntelliJ ▶ 버튼으로는 이 클래스가 아니라 {@link SetupWizardFxLauncher}를 실행하세요.</b>
 * 이 클래스는 {@link Application}을 상속해서, 클래스패스로 실행하면 JVM이
 * "JavaFX 런타임 구성요소가 누락되었습니다"라며 거절합니다. 이유는 런처 클래스 설명에 적어 뒀습니다.
 */
public final class SetupWizardFx extends Application {

    // ── 화면 구성 상수 ───────────────────────────────────

    private static final int PAGE_WELCOME = 0;
    private static final int PAGE_STORAGE = 1;
    private static final int PAGE_DATABASE = 2;
    private static final int PAGE_OPTIONS = 3;
    private static final int PAGE_RUN = 4;
    private static final int PAGE_DONE = 5;

    private static final String[] RAIL_TITLES = {
            "시작", "저장소 선택", "MySQL 접속", "앱 설정", "설치", "완료"
    };

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_DATABASE = "safefood";
    private static final String DEFAULT_MYSQL_PORT = "3306";
    private static final String DEFAULT_SOCKET_PORT = "5000";

    /** 데이터베이스 이름에 허용할 문자. {@link Ui}의 규칙과 같아야 합니다. */
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9_]{1,64}";

    // ── 상태 ─────────────────────────────────────────────

    private Path projectRoot;
    /** JDBC 드라이버를 쓸 수 있는지. 확인 전에는 null */
    private Boolean driverReady;
    /** 연결 테스트 결과. 아직 안 해봤으면 null */
    private Boolean connectionOk;
    private SetupResult result;
    private int currentPage = PAGE_WELCOME;
    private boolean running;

    // ── 컨트롤 ───────────────────────────────────────────

    private final List<HBox> railRows = new ArrayList<>();
    private final List<Node> pages = new ArrayList<>();
    private StackPane pageStack;

    private Label headerTitle;
    private Label headerSubtitle;

    private Label driverStatus;
    private ToggleGroup storageMode;
    private RadioButton mysqlChoice;
    private RadioButton fileChoice;

    private TextField hostField;
    private TextField portField;
    private TextField databaseField;
    private TextField userField;
    private PasswordField passwordField;
    private Button testButton;
    private Label testStatus;
    private Label dbNote;
    private Label dbError;

    private TextField mapKeyField;
    private TextField socketHostField;
    private TextField socketPortField;
    private CheckBox overwriteBox;
    /** config.properties가 이미 있을 때만 보여 줄 카드 */
    private VBox overwriteCard;
    private Label optionsError;

    private ProgressBar progressBar;
    private Label progressLabel;
    private VBox logBox;
    private ScrollPane logScroll;

    private VBox doneBox;

    private Button backButton;
    private Button nextButton;
    private Label navHint;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        projectRoot = ConfigFileWriter.findProjectRoot();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("wizard-root");
        root.setTop(buildHeader());
        root.setLeft(buildRail());
        root.setCenter(buildPages());
        root.setBottom(buildNav());

        Scene scene = new Scene(root, 940, 660);
        URL css = getClass().getResource("setup-wizard.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("SafeFood 개발 환경 설정 마법사");
        stage.setMinWidth(880);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();

        showPage(PAGE_WELCOME);
        checkDriverInBackground();
    }

    // ── 머리말 ───────────────────────────────────────────

    private Node buildHeader() {
        headerTitle = new Label("SafeFood 개발 환경 설정");
        headerTitle.getStyleClass().add("header-title");

        headerSubtitle = new Label("MySQL 준비부터 config.properties 작성까지 한 번에 처리합니다.");
        headerSubtitle.getStyleClass().add("header-subtitle");

        VBox box = new VBox(4, headerTitle, headerSubtitle);
        box.getStyleClass().add("header");
        return box;
    }

    // ── 왼쪽 단계 목록 ───────────────────────────────────

    private Node buildRail() {
        VBox rail = new VBox(2);
        rail.getStyleClass().add("rail");

        for (int i = 0; i < RAIL_TITLES.length; i++) {
            Label badge = new Label(String.valueOf(i + 1));
            badge.getStyleClass().add("rail-badge");

            Label title = new Label(RAIL_TITLES[i]);
            title.getStyleClass().add("rail-label");

            HBox row = new HBox(10, badge, title);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("rail-row");
            railRows.add(row);
            rail.getChildren().add(row);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("여러 번 실행해도 안전합니다.\n이미 있는 것은 건드리지 않습니다.");
        footer.getStyleClass().add("rail-footer");
        footer.setWrapText(true);

        rail.getChildren().addAll(spacer, footer);
        return rail;
    }

    // ── 가운데 페이지 ────────────────────────────────────

    private Node buildPages() {
        pages.add(buildWelcomePage());
        pages.add(buildStoragePage());
        pages.add(buildDatabasePage());
        pages.add(buildOptionsPage());
        pages.add(buildRunPage());
        pages.add(buildDonePage());

        pageStack = new StackPane();
        pageStack.getStyleClass().add("page-stack");
        pageStack.getChildren().addAll(pages);
        pageStack.setAlignment(Pos.TOP_LEFT);
        return pageStack;
    }

    private Node buildWelcomePage() {
        VBox page = page("무엇을 하나요?",
                "저장소를 클론한 뒤 한 번만 실행하면 됩니다. 아래 순서로 진행합니다.");

        VBox list = new VBox(8,
                bullet("1", "JDBC 드라이버 확인 — Maven이 이미 준비해 둡니다."),
                bullet("2", "MySQL 접속 확인 — 연결되지 않으면 data/ 폴더 방식으로 갑니다."),
                bullet("3", "데이터베이스 · 테이블 18개 생성"),
                bullet("4", "알레르기 · 기분 태그 기본 데이터 삽입"),
                bullet("5", "data/public · data/private 폴더와 기본 파일 준비"),
                bullet("6", "config.properties 작성 + .gitignore 확인"));
        list.getStyleClass().add("card");

        Label rootLabel = new Label("프로젝트 경로");
        rootLabel.getStyleClass().add("field-label");
        Label rootValue = new Label(projectRoot.toString());
        rootValue.getStyleClass().add("path-value");
        rootValue.setWrapText(true);

        VBox rootBox = new VBox(4, rootLabel, rootValue);
        rootBox.getStyleClass().add("card");

        page.getChildren().addAll(list, rootBox,
                hint("이미 만들어진 DB · 테이블 · 데이터 · 파일은 그대로 둡니다. 지우는 작업은 하지 않습니다."));
        return page;
    }

    private Node buildStoragePage() {
        VBox page = page("데이터를 어디에 저장할까요?",
                "MySQL이 없어도 괜찮습니다. 나중에 켜고 다시 실행하면 이어서 준비합니다.");

        driverStatus = new Label("JDBC 드라이버 확인 중...");
        driverStatus.getStyleClass().addAll("badge", "badge-idle");

        storageMode = new ToggleGroup();
        mysqlChoice = new RadioButton("MySQL 사용 (권장)");
        mysqlChoice.setToggleGroup(storageMode);
        mysqlChoice.setSelected(true);
        mysqlChoice.getStyleClass().add("choice");

        fileChoice = new RadioButton("MySQL 없이 data/ 폴더만 사용");
        fileChoice.setToggleGroup(storageMode);
        fileChoice.getStyleClass().add("choice");

        VBox mysqlCard = new VBox(6, mysqlChoice,
                sub("데이터베이스와 테이블을 만들고 기본 데이터를 넣습니다. 다음 화면에서 접속 정보를 확인합니다."));
        mysqlCard.getStyleClass().add("card");

        VBox fileCard = new VBox(6, fileChoice,
                sub("DB 단계를 건너뛰고 data/public · data/private에 텍스트 파일로 저장합니다. "
                        + "UI · 소켓 · 추천 로직만 먼저 돌려 볼 때 편합니다."));
        fileCard.getStyleClass().add("card");

        storageMode.selectedToggleProperty().addListener((observable, before, after) -> updateDbPageMode());

        page.getChildren().addAll(driverStatus, mysqlCard, fileCard);
        return page;
    }

    private Node buildDatabasePage() {
        VBox page = page("MySQL 접속 정보",
                "config.properties에 적을 값입니다. 연결 테스트로 미리 확인해 보세요.");

        hostField = new TextField(DEFAULT_HOST);
        portField = new TextField(DEFAULT_MYSQL_PORT);
        databaseField = new TextField(DEFAULT_DATABASE);
        userField = new TextField("root");
        passwordField = new PasswordField();
        passwordField.setPromptText("입력한 글자는 화면에 보이지 않습니다");

        GridPane form = new GridPane();
        form.getStyleClass().add("form");
        form.setHgap(12);
        form.setVgap(10);
        addRow(form, 0, "호스트", hostField);
        addRow(form, 1, "포트", portField);
        addRow(form, 2, "데이터베이스 이름", databaseField);
        addRow(form, 3, "계정", userField);
        addRow(form, 4, "비밀번호", passwordField);

        testButton = new Button("연결 테스트");
        testButton.getStyleClass().add("ghost");
        testButton.setOnAction(event -> testConnection());

        testStatus = new Label("아직 확인하지 않았습니다.");
        testStatus.getStyleClass().addAll("badge", "badge-idle");

        HBox testRow = new HBox(10, testButton, testStatus);
        testRow.setAlignment(Pos.CENTER_LEFT);

        dbError = new Label();
        dbError.getStyleClass().add("error");
        dbError.setVisible(false);
        dbError.setManaged(false);

        VBox card = new VBox(14, form, testRow, dbError);
        card.getStyleClass().add("card");

        dbNote = new Label();
        dbNote.getStyleClass().add("hint");
        dbNote.setWrapText(true);

        page.getChildren().addAll(card, dbNote);
        return page;
    }

    private Node buildOptionsPage() {
        VBox page = page("앱 설정",
                "지금 몰라도 됩니다. 비워 두고 나중에 config.properties에서 고쳐도 됩니다.");

        mapKeyField = new TextField();
        mapKeyField.setPromptText("아직 없으면 비워 두세요");
        socketHostField = new TextField(DEFAULT_HOST);
        socketPortField = new TextField(DEFAULT_SOCKET_PORT);

        GridPane form = new GridPane();
        form.getStyleClass().add("form");
        form.setHgap(12);
        form.setVgap(10);
        addRow(form, 0, "지도 API 키", mapKeyField);
        addRow(form, 1, "소켓 서버 호스트", socketHostField);
        addRow(form, 2, "소켓 서버 포트", socketPortField);

        optionsError = new Label();
        optionsError.getStyleClass().add("error");
        optionsError.setVisible(false);
        optionsError.setManaged(false);

        VBox card = new VBox(14, form, optionsError);
        card.getStyleClass().add("card");

        overwriteBox = new CheckBox("이미 있는 config.properties를 덮어쓰기 (기존 파일은 .bak으로 백업)");
        overwriteBox.getStyleClass().add("choice");

        overwriteCard = new VBox(6, overwriteBox,
                sub("체크하지 않으면 기존 파일을 그대로 둡니다."));
        overwriteCard.getStyleClass().add("card");
        overwriteCard.setVisible(false);
        overwriteCard.setManaged(false);

        page.getChildren().addAll(card, overwriteCard,
                hint("그룹 추천은 방장 앱이 곧 서버입니다. 소켓 포트는 비어 있는 포트면 무엇이든 됩니다."));
        return page;
    }

    private Node buildRunPage() {
        VBox page = page("설치", "아래 [설치 시작]을 누르면 3~7단계를 실행합니다.");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress");

        progressLabel = new Label("대기 중");
        progressLabel.getStyleClass().add("progress-label");

        logBox = new VBox(2);
        logBox.getStyleClass().add("log-box");

        logScroll = new ScrollPane(logBox);
        logScroll.getStyleClass().add("log-scroll");
        logScroll.setFitToWidth(true);
        VBox.setVgrow(logScroll, Priority.ALWAYS);
        // 새 줄이 추가될 때마다 맨 아래로 따라 내려갑니다.
        logBox.heightProperty().addListener((observable, before, after) -> logScroll.setVvalue(1.0));

        VBox card = new VBox(10, progressLabel, progressBar, logScroll);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        page.getChildren().add(card);
        return page;
    }

    private Node buildDonePage() {
        VBox page = page("완료", "설정 결과입니다.");
        doneBox = new VBox(10);
        page.getChildren().add(doneBox);
        return page;
    }

    // ── 아래 내비게이션 ──────────────────────────────────

    private Node buildNav() {
        backButton = new Button("뒤로");
        backButton.getStyleClass().add("ghost");
        backButton.setOnAction(event -> goBack());

        nextButton = new Button("다음");
        nextButton.getStyleClass().add("primary");
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(event -> goNext());

        navHint = new Label();
        navHint.getStyleClass().add("nav-hint");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(10, navHint, spacer, backButton, nextButton);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.getStyleClass().add("nav");
        return nav;
    }

    private void goBack() {
        if (currentPage > PAGE_WELCOME) {
            showPage(currentPage - 1);
        }
    }

    private void goNext() {
        switch (currentPage) {
            case PAGE_DATABASE -> {
                if (validateDatabasePage()) {
                    showPage(PAGE_OPTIONS);
                }
            }
            case PAGE_OPTIONS -> {
                if (validateOptionsPage()) {
                    showPage(PAGE_RUN);
                }
            }
            case PAGE_RUN -> runSetup();
            case PAGE_DONE -> Platform.exit();
            default -> showPage(currentPage + 1);
        }
    }

    // ── 페이지 전환 ──────────────────────────────────────

    private void showPage(int index) {
        currentPage = index;

        for (int i = 0; i < pages.size(); i++) {
            boolean visible = i == index;
            pages.get(i).setVisible(visible);
            pages.get(i).setManaged(visible);
        }
        for (int i = 0; i < railRows.size(); i++) {
            HBox row = railRows.get(i);
            row.getStyleClass().removeAll("active", "done");
            if (i == index) {
                row.getStyleClass().add("active");
            } else if (i < index) {
                row.getStyleClass().add("done");
            }
        }

        if (index == PAGE_DATABASE) {
            updateDbPageMode();
        }
        if (index == PAGE_OPTIONS) {
            refreshOverwriteOption();
        }
        updateNav();
    }

    private void updateNav() {
        backButton.setDisable(running || currentPage == PAGE_WELCOME || currentPage == PAGE_DONE);
        nextButton.setDisable(running);

        switch (currentPage) {
            case PAGE_RUN -> {
                nextButton.setText(result == null ? "설치 시작" : "다시 실행");
                navHint.setText(running ? "설치 중입니다. 창을 닫지 마세요." : "누르기 전까지는 아무것도 바뀌지 않습니다.");
            }
            case PAGE_DONE -> {
                nextButton.setText("닫기");
                navHint.setText("");
            }
            default -> {
                nextButton.setText("다음");
                navHint.setText("");
            }
        }
    }

    /** 저장소 선택에 따라 MySQL 페이지의 안내 문구와 연결 테스트 버튼을 바꿉니다. */
    private void updateDbPageMode() {
        if (dbNote == null) {
            return;
        }
        if (usesDatabase()) {
            dbNote.setText("연결 테스트를 건너뛰고 진행해도 됩니다. "
                    + "연결되지 않으면 DB 단계를 건너뛰고 data/ 폴더에 저장합니다.");
            testButton.setDisable(false);
        } else {
            dbNote.setText("지금은 data/ 폴더 방식이라 접속하지 않습니다. "
                    + "여기 적은 값은 나중에 쓸 수 있도록 config.properties에만 기록해 둡니다.");
            testButton.setDisable(true);
            testStatus.setText("사용 안 함");
            setBadge(testStatus, "badge-idle");
        }
    }

    /** config.properties가 이미 있을 때만 덮어쓰기 선택지를 보여 줍니다. */
    private void refreshOverwriteOption() {
        boolean exists = Files.exists(ConfigFileWriter.configPath(projectRoot));
        overwriteCard.setVisible(exists);
        overwriteCard.setManaged(exists);
        if (!exists) {
            overwriteBox.setSelected(false);
        }
    }

    // ── 1단계: 드라이버 확인 ─────────────────────────────

    /**
     * 드라이버가 없으면 Maven Central에서 내려받기까지 하므로 시간이 걸릴 수 있습니다.
     * 창이 멈추지 않도록 백그라운드에서 확인합니다.
     */
    private void checkDriverInBackground() {
        StringBuilder captured = new StringBuilder();
        SetupReporter collector = collectingReporter(captured);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return JdbcDriverSetup.ensureReady(projectRoot, collector);
            }
        };
        task.setOnSucceeded(event -> applyDriverState(task.getValue(), captured.toString().trim()));
        task.setOnFailed(event -> applyDriverState(false, String.valueOf(task.getException())));
        runInBackground(task, "safefood-driver-check");
    }

    private void applyDriverState(boolean ready, String message) {
        driverReady = ready;
        if (ready) {
            driverStatus.setText("JDBC 드라이버 준비 완료");
            setBadge(driverStatus, "badge-ok");
        } else {
            driverStatus.setText("JDBC 드라이버를 쓸 수 없습니다 — data/ 폴더 방식으로만 진행합니다");
            setBadge(driverStatus, "badge-fail");
            mysqlChoice.setDisable(true);
            fileChoice.setSelected(true);
            if (!message.isEmpty()) {
                // 실패 원인은 길어서 배지에 다 넣지 않고 마우스를 올렸을 때 보여 줍니다.
                driverStatus.setTooltip(new Tooltip(message));
            }
        }
    }

    // ── 2단계: 연결 테스트 ───────────────────────────────

    private void testConnection() {
        if (!validateDatabasePage()) {
            return;
        }
        DbConfig config = readDbConfig();

        testButton.setDisable(true);
        testStatus.setText("확인 중...");
        setBadge(testStatus, "badge-idle");

        StringBuilder captured = new StringBuilder();
        SetupReporter collector = collectingReporter(captured);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                new DatabaseInitializer(config, collector).testConnection();
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            connectionOk = true;
            String version = captured.toString().trim();
            testStatus.setText(version.isEmpty() ? "연결 성공" : "연결 성공 — " + version);
            setBadge(testStatus, "badge-ok");
            testButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            connectionOk = false;
            Throwable error = task.getException();
            String reason = error instanceof SQLException sql
                    ? DatabaseInitializer.explain(sql)
                    : String.valueOf(error.getMessage());
            testStatus.setText("연결 실패 — " + reason);
            setBadge(testStatus, "badge-fail");
            testButton.setDisable(false);
        });
        runInBackground(task, "safefood-connection-test");
    }

    // ── 3~7단계: 실행 ────────────────────────────────────

    private void runSetup() {
        SetupRequest request = new SetupRequest(
                projectRoot,
                readDbConfig(),
                usesDatabase(),
                mapKeyField.getText().trim(),
                socketHostField.getText().trim(),
                Integer.parseInt(socketPortField.getText().trim()),
                overwriteBox.isSelected());

        logBox.getChildren().clear();
        progressBar.setProgress(0);
        setRunning(true);
        appendLog("log-info", "프로젝트 경로: " + projectRoot);

        Task<SetupResult> task = new Task<>() {
            @Override
            protected SetupResult call() throws Exception {
                return new SetupService(fxReporter()).run(request);
            }
        };
        task.setOnSucceeded(event -> {
            result = task.getValue();
            progressBar.setProgress(1);
            progressLabel.setText("완료");
            setRunning(false);
            fillDonePage(request, result);
            showPage(PAGE_DONE);
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            appendLog("log-fail", "✕ 중단됐습니다: " + error);
            appendLog("log-detail", "프로젝트 폴더에 쓰기 권한이 있는지 확인해 주세요.");
            progressLabel.setText("실패");
            setRunning(false);
        });
        runInBackground(task, "safefood-setup");
    }

    private void setRunning(boolean value) {
        running = value;
        updateNav();
    }

    private void fillDonePage(SetupRequest request, SetupResult done) {
        doneBox.getChildren().clear();

        doneBox.getChildren().addAll(
                resultRow(done.databaseDone(),
                        "데이터베이스 " + request.db().database(),
                        done.databaseDone()
                                ? "테이블 + 기본 데이터까지 준비됐습니다."
                                : "준비되지 않았습니다. MySQL을 켜고 다시 실행하면 이어서 진행합니다."),
                resultRow(true,
                        "data/ 폴더",
                        done.dataFilesMade() == 0
                                ? "이미 준비돼 있어 건드리지 않았습니다."
                                : "새 파일 " + done.dataFilesMade() + "개를 만들었습니다."),
                resultRow(done.configWritten(),
                        "config.properties",
                        done.configWritten()
                                ? "새로 작성했습니다."
                                : "기존 파일을 그대로 두었습니다."));

        VBox next = new VBox(6);
        next.getStyleClass().add("card");
        Label nextTitle = new Label("다음 단계");
        nextTitle.getStyleClass().add("card-title");
        next.getChildren().add(nextTitle);
        if (!done.databaseDone()) {
            next.getChildren().add(sub("· 지금은 data/ 폴더에 정보를 저장합니다. MySQL을 켜고 다시 실행하면 이어서 준비합니다."));
        }
        if (request.mapApiKey().isEmpty()) {
            next.getChildren().add(sub("· 지도 API 키를 발급받으면 config.properties의 map.api.key에 채워 넣으세요."));
        }
        next.getChildren().addAll(
                sub("· config.properties와 data/private/는 절대 커밋하지 마세요."),
                sub("· 자세한 설명은 docs/SetupWizard.md에 있습니다."));

        Button openFolder = new Button("프로젝트 폴더 열기");
        openFolder.getStyleClass().add("ghost");
        openFolder.setOnAction(event -> getHostServices().showDocument(projectRoot.toUri().toString()));

        doneBox.getChildren().addAll(next, openFolder);
    }

    private Node resultRow(boolean success, String title, String description) {
        Label mark = new Label(success ? "완료" : "미완");
        mark.getStyleClass().addAll("badge", success ? "badge-ok" : "badge-warn");

        Label name = new Label(title);
        name.getStyleClass().add("card-title");

        Label detail = new Label(description);
        detail.getStyleClass().add("sub");
        detail.setWrapText(true);

        VBox text = new VBox(2, name, detail);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, mark, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        return row;
    }

    // ── 로그 ─────────────────────────────────────────────

    /** {@link SetupService}가 부르는 창구. 반드시 FX 스레드로 넘겨서 화면을 고칩니다. */
    private SetupReporter fxReporter() {
        return new SetupReporter() {
            @Override
            public void step(int number, int total, String title) {
                Platform.runLater(() -> {
                    progressBar.setProgress((double) number / total);
                    progressLabel.setText("[" + number + "/" + total + "] " + title);
                    appendLog("log-step", "[" + number + "/" + total + "] " + title);
                });
            }

            @Override
            public void ok(String message) {
                Platform.runLater(() -> appendLog("log-ok", "✓ " + message));
            }

            @Override
            public void warn(String message) {
                Platform.runLater(() -> appendLog("log-warn", "! " + message));
            }

            @Override
            public void fail(String message) {
                Platform.runLater(() -> appendLog("log-fail", "✕ " + message));
            }

            @Override
            public void info(String message) {
                Platform.runLater(() -> appendLog("log-info", message));
            }

            @Override
            public void detail(String message) {
                Platform.runLater(() -> appendLog("log-detail", "   " + message));
            }
        };
    }

    private void appendLog(String styleClass, String message) {
        Label line = new Label(message);
        line.getStyleClass().addAll("log-line", styleClass);
        line.setWrapText(true);
        logBox.getChildren().add(line);
    }

    /**
     * 화면에 띄우지 않고 메시지만 모아 두는 창구.
     * 연결 테스트의 MySQL 버전처럼 <b>결과 한 줄</b>만 필요할 때 씁니다.
     */
    private static SetupReporter collectingReporter(StringBuilder target) {
        return new SetupReporter() {
            @Override
            public void step(int number, int total, String title) {
                append(title);
            }

            @Override
            public void ok(String message) {
                append(message);
            }

            @Override
            public void warn(String message) {
                append(message);
            }

            @Override
            public void fail(String message) {
                append(message);
            }

            @Override
            public void info(String message) {
                append(message);
            }

            @Override
            public void detail(String message) {
                append(message);
            }

            private void append(String message) {
                // 백그라운드 스레드에서 불리므로 StringBuilder 접근을 막아 둡니다.
                synchronized (target) {
                    target.append(message).append(System.lineSeparator());
                }
            }
        };
    }

    // ── 입력값 읽기 · 검사 ───────────────────────────────

    private boolean usesDatabase() {
        return mysqlChoice.isSelected() && Boolean.TRUE.equals(driverReady);
    }

    private DbConfig readDbConfig() {
        return new DbConfig(
                hostField.getText().trim(),
                Integer.parseInt(portField.getText().trim()),
                databaseField.getText().trim(),
                userField.getText().trim(),
                passwordField.getText());
    }

    private boolean validateDatabasePage() {
        if (hostField.getText().trim().isEmpty()) {
            return showError(dbError, "호스트를 입력하세요.");
        }
        if (!isValidPort(portField.getText())) {
            return showError(dbError, "포트는 1 ~ 65535 사이의 숫자여야 합니다.");
        }
        if (!databaseField.getText().trim().matches(IDENTIFIER_PATTERN)) {
            // SQL에 그대로 이어 붙이는 값이라 반드시 걸러야 합니다. (Ui.askIdentifier와 같은 규칙)
            return showError(dbError, "데이터베이스 이름은 영문·숫자·밑줄(_)만 쓸 수 있습니다. (최대 64자)");
        }
        if (userField.getText().trim().isEmpty()) {
            return showError(dbError, "계정을 입력하세요.");
        }
        return hideError(dbError);
    }

    private boolean validateOptionsPage() {
        if (socketHostField.getText().trim().isEmpty()) {
            return showError(optionsError, "소켓 서버 호스트를 입력하세요.");
        }
        if (!isValidPort(socketPortField.getText())) {
            return showError(optionsError, "소켓 포트는 1 ~ 65535 사이의 숫자여야 합니다.");
        }
        return hideError(optionsError);
    }

    private static boolean isValidPort(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return value >= 1 && value <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        return false;
    }

    private static boolean hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
        return true;
    }

    // ── 작은 도우미 ──────────────────────────────────────

    private static void runInBackground(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        // 사용자가 창을 닫으면 작업도 함께 끝나도록 데몬으로 둡니다.
        thread.setDaemon(true);
        thread.start();
    }

    private static VBox page(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("page-desc");
        descriptionLabel.setWrapText(true);

        VBox box = new VBox(12, titleLabel, descriptionLabel);
        box.getStyleClass().add("page");
        box.setPadding(new Insets(24, 28, 24, 28));
        return box;
    }

    private static void addRow(GridPane form, int row, String label, Node field) {
        Label name = new Label(label);
        name.getStyleClass().add("field-label");
        form.add(name, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static Node bullet(String number, String text) {
        Label badge = new Label(number);
        badge.getStyleClass().add("bullet-badge");

        Label body = new Label(text);
        body.getStyleClass().add("sub");
        body.setWrapText(true);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox row = new HBox(10, badge, body);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label sub(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sub");
        label.setWrapText(true);
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setWrapText(true);
        return label;
    }

    private static void setBadge(Label label, String badgeClass) {
        label.getStyleClass().removeAll("badge-ok", "badge-fail", "badge-idle", "badge-warn");
        label.getStyleClass().add(badgeClass);
    }
}
