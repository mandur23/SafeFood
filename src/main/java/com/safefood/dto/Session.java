package com.safefood.dto;

public class Session {
    private static UserDto currentUser;


    // 유저 정보를 저장함
    public static void setCurrentUser(UserDto user){
        currentUser = user;
    }

    // 유저 정보를 전달함
    public static UserDto getCurrentUser(){
        return currentUser;
    }

    // 로그아웃 시 유저 정보를 지움
    public static void logout(){
        currentUser = null;
    }
}
