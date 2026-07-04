package com.aezora.music.data.local

import com.aezora.music.domain.model.*

fun TrackEntity.toDomain() = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    duration = duration,
    source = TrackSource.valueOf(source),
    streamUrl = streamUrl,
    localPath = localPath,
    isLiked = isLiked,
    isDownloaded = isDownloaded,
    hasDrm = hasDrm,
    genre = genre,
    playCount = playCount,
    addedAt = addedAt
)

fun Track.toEntity() = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    duration = duration,
    source = source.name,
    streamUrl = streamUrl,
    localPath = localPath,
    isLiked = isLiked,
    isDownloaded = isDownloaded,
    hasDrm = hasDrm,
    genre = genre,
    playCount = playCount,
    addedAt = addedAt
)

fun PlaylistWithTracks.toDomain() = Playlist(
    id = playlist.id,
    name = playlist.name,
    description = playlist.description,
    artworkUrl = playlist.artworkUrl,
    tracks = tracks.map { it.toDomain() },
    isUserCreated = playlist.isUserCreated,
    source = PlaylistSource.valueOf(playlist.source),
    createdAt = playlist.createdAt
)

fun Playlist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    artworkUrl = artworkUrl,
    isUserCreated = isUserCreated,
    source = source.name,
    createdAt = createdAt
)
