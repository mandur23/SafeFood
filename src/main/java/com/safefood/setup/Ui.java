package com.safefood.setup;

import com.safefood.setup.core.SetupReporter;
import com.safefood.setup.core.SetupService;

import java.io.Console;
import java.util.Scanner;

/**
 * 콘솔 출력·입력 헬퍼.
 *
 * <p>IntelliJ 실행창에서는 {@link System#console()}이 null이라 비밀번호를 가려서 받을 수 없습니다.
 * 그래서 콘솔이 없으면 Scanner로 대체하고, 입력이 화면에 보인다는 사실을 미리 알려 줍니다.
 *
 * <p>출력 쪽은 {@link #reporter()}로 {@link SetupReporter} 형태로도 꺼내 쓸 수 있습니다.
 * 처리 담당 클래스({@link SetupService} 등)는 이 인터페이스만 알기 때문에,
 * 같은 로직을 JavaFX 화면({@link SetupWizardFx})에서도 그대로 씁니다.
 */
final class Ui {

    /** 데이터베이스·테이블 이름에 허용할 문자 (SQL 문자열로 직접 이어 붙이기 때문에 반드시 검사) */
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9_]{1,64}";

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Console CONSOLE = System.console();

    /** 처리 담당 클래스에 넘겨 줄 출력 창구. 아래 static 메서드로 그대로 이어집니다. */
    private static final SetupReporter REPORTER = new SetupReporter() {
        @Override
        public void step(int number, int total, String title) {
            Ui.step(number, total, title);
        }

        @Override
        public void ok(String message) {
            Ui.ok(message);
        }

        @Override
        public void warn(String message) {
            Ui.warn(message);
        }

        @Override
        public void fail(String message) {
            Ui.fail(message);
        }

        @Override
        public void info(String message) {
            Ui.info(message);
        }

        @Override
        public void detail(String message) {
            Ui.detail(message);
        }
    };

    private Ui() {
    }

    static SetupReporter reporter() {
        return REPORTER;
    }

    // ── 출력 ─────────────────────────────────────────────

    static void banner() {
        System.out.println();
        System.out.println("=============================================");
        System.out.println("   SafeFood 개발 환경 설정 마법사");
        System.out.println("=============================================");
        System.out.println("  MySQL 연결 → DB·테이블 생성 → 설정 파일 작성을 한 번에 처리합니다.");
        System.out.println("  대괄호 [] 안이 기본값입니다. 그냥 Enter를 누르면 기본값이 쓰입니다.");
        System.out.println("  여러 번 실행해도 안전합니다. 이미 있는 것은 건너뜁니다.");
        System.out.println();
        System.out.println("  창으로 된 화면을 쓰려면: mvn javafx:run -Pwizard-fx");
    }

    static void step(int number, int total, String title) {
        System.out.println();
        System.out.printf("[%d/%d] %s%n", number, total, title);
        System.out.println("---------------------------------------------");
    }

    static void section(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("---------------------------------------------");
    }

    static void ok(String message) {
        System.out.println("  [OK] " + message);
    }

    static void warn(String message) {
        System.out.println("  [!]  " + message);
    }

    static void fail(String message) {
        System.out.println("  [X]  " + message);
    }

    static void info(String message) {
        System.out.println("  " + message);
    }

    /** 목록·부연 설명처럼 한 단계 들여쓸 내용 */
    static void detail(String message) {
        System.out.println("       " + message);
    }

    static void blank() {
        System.out.println();
    }

    // ── 입력 ─────────────────────────────────────────────

    static String ask(String label, String defaultValue) {
        System.out.print("  " + label + " [" + defaultValue + "]: ");
        String input = SCANNER.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    /** 비워 둘 수 있는 입력 (예: 아직 발급받지 않은 API 키) */
    static String askOptional(String label) {
        System.out.print("  " + label + " (없으면 Enter): ");
        return SCANNER.nextLine().trim();
    }

    static int askPort(String label, int defaultValue) {
        while (true) {
            String input = ask(label, String.valueOf(defaultValue));
            try {
                int value = Integer.parseInt(input);
                if (value >= 1 && value <= 65535) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // 아래 안내 문구로 처리
            }
            warn("1 ~ 65535 사이의 숫자를 입력하세요.");
        }
    }

    static String askIdentifier(String label, String defaultValue) {
        while (true) {
            String input = ask(label, defaultValue);
            if (input.matches(IDENTIFIER_PATTERN)) {
                return input;
            }
            warn("영문·숫자·밑줄(_)만 쓸 수 있습니다. (최대 64자)");
        }
    }

    static String askPassword(String label) {
        if (CONSOLE != null) {
            char[] typed = CONSOLE.readPassword("  " + label + ": ");
            return typed == null ? "" : new String(typed);
        }
        System.out.print("  " + label + " (※ 입력한 글자가 화면에 그대로 보입니다): ");
        return SCANNER.nextLine();
    }

    static boolean confirm(String question, boolean defaultYes) {
        String hint = defaultYes ? "Y/n" : "y/N";
        while (true) {
            System.out.print("  " + question + " (" + hint + "): ");
            String input = SCANNER.nextLine().trim().toLowerCase();
            if (input.isEmpty()) {
                return defaultYes;
            }
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            warn("y 또는 n으로 답해 주세요.");
        }
    }

    static void close() {
        SCANNER.close();
    }
}
