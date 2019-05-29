package com.st.faceplusplus.api;

import com.st.faceplusplus.activity.CompareResp;
import com.st.faceplusplus.activity.DetectResp;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface FaceApi {

    String BASE_URL = "https://api-cn.faceplusplus.com/facepp/v3/";
    String DETECT = "detect";
    String ADD_FACE = "faceset/addface";
    String COMPARE = "compare";

    @FormUrlEncoded
    @POST(DETECT)
    Observable<DetectResp> detect(@Field("api_key") String api_key,
                                  @Field("api_secret") String api_secret,
                                  @Field("return_landmark") int return_landmark,
                                  @Field("image_base64") String image_base64);


    @FormUrlEncoded
    @POST(ADD_FACE)
    Observable<ResponseBody> addFace(@Field("api_key") String api_key,
                               @Field("api_secret") String api_secret,
                               @Field("faceset_token") String faceset_token,
                               @Field("face_tokens") String face_tokens);

    @FormUrlEncoded
    @POST(COMPARE)
    Observable<CompareResp> compare(@Field("api_key") String api_key,
                                    @Field("api_secret") String api_secret,
                                    @Field("face_token1") String face_token1,
                                    @Field("face_token2") String face_token2);

}
