package com.safefood.service;

import com.safefood.dao.FeedbackDao;

public class FeedbackService {
    private final FeedbackDao feedbackDao = new FeedbackDao();
    private final com.safefood.dao.HistoryDao historyDao = new com.safefood.dao.HistoryDao();

    public boolean saveFeedback(int userId, int historyId, int menuId, int rating) {
        // 평점이 4점 이상이면 좋아요(liked)로 처리
        boolean liked = rating >= 4;
        
        // 1. 피드백 저장 및 생성된 feedback_id 받아오기
        int feedbackId = feedbackDao.insertFeedback(userId, menuId, liked, rating);
        if (feedbackId == -1) return false;
        
        // 2. 히스토리에 feedback_id 업데이트하기
        return historyDao.updateFeedbackId(historyId, feedbackId);
    }
}
