package com.safefood.setup.core;

/**
 * 마법사 실행 결과. 마지막 요약 화면을 그리는 데 씁니다.
 *
 * @param databaseReady  데이터베이스 생성까지 성공
 * @param tablesReady    테이블 생성까지 성공
 * @param masterDataDone 기본 데이터(알레르기·기분 태그) 삽입까지 성공
 * @param dataFilesMade  {@code data/}에 새로 만든 파일 수 (이미 있던 파일은 세지 않음)
 * @param configWritten  config.properties를 새로 쓰거나 덮어썼으면 true (기존 파일 유지면 false)
 */
public record SetupResult(boolean databaseReady,
                          boolean tablesReady,
                          boolean masterDataDone,
                          int dataFilesMade,
                          boolean configWritten) {

    /** DB 준비가 끝까지 성공했는지 (테이블 + 기본 데이터) */
    public boolean databaseDone() {
        return databaseReady && tablesReady && masterDataDone;
    }
}
