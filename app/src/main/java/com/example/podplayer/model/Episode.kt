package com.example.podplayer.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*


/*
Here, you’re adding some new attributes to define a
foreign key and an index on the database
a single ForeignKey that relates the podcastId property
in the Episode entity to the property id in the Podcast entity
1.entity: Defines the parent entity.
2. parentColumns: Defines the column names on the parent entity (the Podcast class).
3. childColumns: Defines the column names in the child entity (the Episode class).
4. onDelete: Defines the behavior when the parent entity is deleted. CASCADE indicates that any time you delete a podcast,
all related child episodes are deleted automatically.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class Episode(
    @PrimaryKey var guid: String = "",
    var podcastId: Long? = null,
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "",
    var mimeType: String = "",
    var releaseDate: Date = Date(),
    var duration: String = ""
)