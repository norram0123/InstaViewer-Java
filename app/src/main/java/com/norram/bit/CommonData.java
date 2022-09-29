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

    static public String requestUrlFormatter() {
        String BUSINESS_ACCOUNT_ID = "[YOUR BUSINESS ACCOUNT ID]"; // TODO : change your business account id
        String MEDIA_FIELDS = "profile_picture_url,name,media%s{media_type,media_url,permalink,children{media_type,media_url}}";
        String ACCESS_TOKEN = "[APP ACCESS TOKEN]"; // TODO : change your app access token
        return "https://graph.facebook.com/v15.0/" + BUSINESS_ACCOUNT_ID + "?fields=business_discovery.username(%s){" + MEDIA_FIELDS + "}&access_token=" + ACCESS_TOKEN;
    }
}