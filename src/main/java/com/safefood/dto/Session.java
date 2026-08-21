package com.safefood.dto;

public class Session {
    private static UserDto currentUser; // 정회원용
    private static PreferenceDto guestPreference; // 게스트용


    // 유저 정보를 저장함
    public static void setCurrentUser(UserDto user){
        currentUser = user;
    }

    // 유저 정보를 전달함
    public static UserDto getCurrentUser(){
        return currentUser;
    }

    // 게스트 정보 저장&전달
    public static void setGuestPreference(PreferenceDto pref){ guestPreference = pref; }
    public static PreferenceDto getGuestPreferences(){ return guestPreference; }

    // 로그아웃 시 유저 정보를 지움
    public static void logout(){
        currentUser = null;
        guestPreference = null;
    }
}
