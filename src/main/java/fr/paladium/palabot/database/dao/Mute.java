package fr.paladium.palabot.database.dao;

import dev.morphia.annotations.*;
import lombok.*;
import org.bson.types.ObjectId;

@Entity("mutes")
@Indexes(
        {
                @Index(fields = @Field("guildID")),
                @Index(fields = @Field("userID")),
                @Index(fields = @Field("end"))
        }
)
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
@Builder
public class Mute {
    @Id
    protected ObjectId id;
    private String guildID;
    private long userID;
    private long end;
    private String reason;
    private String proof;
    private boolean ended;
}
