package com.norram.bit;

public class CommonData {
    private static CommonData instance = null;

    private CommonData() {}

    // return sole instance
    public static CommonData getInstance() {
        if (instance == null) {
            instance = new CommonData();
        }
        return instance;
    }

    boolean isMeasured = false;
    int screenWidth;
    String chosenUrl;
    String chosenPermalink;

    String BUSINESS_ACCOUNT_ID = "[YOUR BUSINESS ACCOUNT ID]"; // TODO(A) : change your business account id
    String ACCESS_TOKEN = "[APP ACCESS TOKEN]"; // TODO(A) : change your app access token

    static public String requestUrlFormatter() {
        return "https://graph.facebook.com/v16.0/%s?fields=business_discovery.username(%s){profile_picture_url,name,media%s{media_type,media_url,permalink,children{media_type,media_url}}}&access_token=%s";
    }
}
