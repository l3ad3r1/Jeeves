package com.l3ad3r1.octojotter.data.local

import com.hermes.agent.data.backup.NoteBackup

/** Convert only notes that are safe to place in an access-controlled cloud Gist. */
fun NoteEntity.toBackupOrNull(): NoteBackup? {
    if (locked || encrypted) return null
    return NoteBackup(
        title = title,
        content = content,
        gistId = gistId,
        pinned = pinned,
        tags = tags,
        folder = folder,
        locked = locked,
        repository = repository,
        path = path,
        sha = sha,
        deletedAt = deletedAt,
        pendingRemoteDelete = pendingRemoteDelete,
        encrypted = encrypted,
        encryptionVersion = encryptionVersion,
        remoteUpdatedAt = remoteUpdatedAt,
        lastSyncedContentHash = lastSyncedContentHash,
        conflictState = conflictState,
        conflictedRemoteContent = conflictedRemoteContent,
        conflictedRemoteModifiedAt = conflictedRemoteModifiedAt,
        needsSync = needsSync,
        createdAt = lastModifiedLocally,
        modifiedAt = lastModifiedLocally,
    )
}

/** Rebuild a note without silently dropping trash, repository, or privacy metadata. */
fun NoteBackup.toRestoredEntity(): NoteEntity = NoteEntity(
    title = title,
    content = content,
    gistId = gistId,
    pinned = pinned,
    tags = tags,
    folder = folder,
    repository = repository,
    path = path,
    sha = sha,
    deletedAt = deletedAt,
    pendingRemoteDelete = pendingRemoteDelete,
    locked = locked,
    encrypted = encrypted,
    encryptionVersion = encryptionVersion,
    remoteUpdatedAt = remoteUpdatedAt,
    lastSyncedContentHash = lastSyncedContentHash,
    conflictState = conflictState,
    conflictedRemoteContent = conflictedRemoteContent,
    conflictedRemoteModifiedAt = conflictedRemoteModifiedAt,
    needsSync = needsSync,
    lastModifiedLocally = modifiedAt,
)
