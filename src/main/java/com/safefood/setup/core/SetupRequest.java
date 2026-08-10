package com.safefood.setup.core;

import java.nio.file.Path;

/**
 * 마법사가 실행할 작업 묶음.
 *
 * <p>화면(콘솔·JavaFX)은 값을 모으는 일만 하고, 실제 처리는 {@link SetupService}가 합니다.
 * 그래서 실행에 필요한 결정은 <b>전부 여기에 담아서</b> 넘깁니다.
 * 실행 도중에 다시 물어보지 않으므로, 처리 중에 화면이 멈추는 일이 없습니다.
 *
 * @param projectRoot     프로젝트 최상위 경로 ({@link ConfigFileWriter#findProjectRoot()})
 * @param db              접속 정보. DB를 쓰지 않아도 config.properties에 적어야 해서 항상 필요합니다.
 * @param useDatabase     false면 DB 단계(3~5)를 건너뛰고 {@code data/}만 준비합니다.
 * @param mapApiKey       지도 API 키 (아직 없으면 빈 문자열)
 * @param socketHost      그룹 실시간 통신용 소켓 서버 호스트
 * @param socketPort      소켓 서버 포트
 * @param overwriteConfig config.properties가 이미 있을 때 덮어쓸지 여부 (false면 기존 파일 유지)
 */
public record SetupRequest(Path projectRoot,
                           DbConfig db,
                           boolean useDatabase,
                           String mapApiKey,
                           String socketHost,
                           int socketPort,
                           boolean overwriteConfig) {
}
