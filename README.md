<h1 align="center">Bit (Java ver.)<br><sub>~Insta Viewer with expand button~</sub></h1>
<p align="center">
<img src="https://user-images.githubusercontent.com/102008212/193114217-7fb93a68-62c8-401a-8e37-d379a08c1f60.png" width="240dp" />
<img src="https://user-images.githubusercontent.com/102008212/193114222-32e045eb-ff49-4742-bed1-4aab5096b93b.png" width="240dp" />
<img src="https://user-images.githubusercontent.com/102008212/193114227-806268e5-7c2c-4ca6-a759-170d2d7aeba1.png" width="240dp" />
</p>

##   <img src="https://user-images.githubusercontent.com/102008212/180079714-0d0af206-38c5-4f0a-a91b-32e1396f9f2a.png" width="26px;" /> Main function

Instagram has a function called "album posting", which allows you to combine multiple photos into one post. However, the viewer has to scroll over and over, which is troublesome. Therefore, I created an application that displays a list of posts with an expand button by searching for a username.
<video controls src="https://user-images.githubusercontent.com/102008212/193116527-44b1b11a-be51-45b2-adf4-c1694433222e.mp4" muted="false"></video>


## 🌐 How to use

This app uses Instagram Graph API. Therefore, you need to get facebook, instagram account, business account id and access token for the app (<a href="https://blog.dtn.jp/2022/02/02/instagram-graph-api-ver12/">this link</a> can help you to get them). In addition, you need to change some code in CommonData.java if you build the program.

```java
public class CommonData {
    // ...
    static public String requestUrlFormatter() {
        String BUSINESS_ACCOUNT_ID = "[YOUR BUSINESS ACCOUNT ID]";
        String MEDIA_FIELDS = "profile_picture_url,name,media%s{media_type,media_url,permalink,children{media_type,media_url}}";
        String ACCESS_TOKEN = "[APP ACCESS TOKEN]";
        return "https://graph.facebook.com/v15.0/" + BUSINESS_ACCOUNT_ID + "?fields=business_discovery.username(%s){" + MEDIA_FIELDS + "}&access_token=" + ACCESS_TOKEN;
    }
}
```
<sub>↑ Change [YOUR BUSINESS ACCOUNT ID] and [APP ACCESS TOKEN]</sub>


## :seedling: Kotlin ver.
These code is written in Java (except data class). In case you are interested in Kotlin code, you can refer to <a href="https://github.com/norram0123/InstaViewer">this repository</a>.


## 👀 Author
**Name**: *Norram*

**Occupation**: *University student* <sub>(Applied Mathematics major)</sub>

**Favorite language**: *Java, Kotlin, C*

- [Github](https://github.com/norram0123)

## License
```
Copyright 2022 norram

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```