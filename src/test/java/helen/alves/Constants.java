package helen.alves;

import io.restassured.http.ContentType;

public interface Constants {
    String APP_BASE_URl = "https://barrigarest.wcaquino.me/";
    Integer APP_PORT = 443;
    String APP_BASE_PATH = "";

    ContentType APP_CONTENT_TYPE = ContentType.JSON;

    Long MAX_TIMEOUT = 4000L;

}
