package com.cse.coari.data

import com.google.gson.annotations.SerializedName

data class AnchorDTOItem (
    @SerializedName("anchor_id") val anchorId: String,
    @SerializedName("buildingName") val buildingName: String,
    @SerializedName("floor") val floor: String,
    @SerializedName("pose") val pose: String,
    @SerializedName("roomContent") val roomContent: String,
    @SerializedName("roomName") val roomName: String,
    @SerializedName("roomNumber") val roomNumber: String,
    @SerializedName("roomVideo") val roomVideo: String
)