package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.HEADER_BASE_URL
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.bean.RepoInfoResp
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

const val VERSION = 3

interface CloudRuleBundleRequest {
  @GET("cloud/md5/v$VERSION")
  suspend fun requestCloudRuleInfo(@Header("Referer") referer: String = BuildConfig.APPLICATION_ID): CloudRuleInfo?

  @Headers("Accept: application/vnd.github.v3+json", "$HEADER_BASE_URL: Repo-Info")
  @GET(ApiManager.GITHUB_API_REPO_INFO)
  suspend fun requestRepoInfo(
    @Header("Owner") owner: String,
    @Header("Repo") repo: String
  ): RepoInfoResp?
}
