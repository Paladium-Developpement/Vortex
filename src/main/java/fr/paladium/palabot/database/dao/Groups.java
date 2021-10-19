package fr.paladium.palabot.database.dao;

import dev.morphia.annotations.*;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.List;

@Entity("groups")
@Indexes(
        {
                @Index(fields = @Field("guildID")),
                @Index(fields = @Field("groupID")),
        }
)
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@Builder
public class Groups {
    @Id
    protected ObjectId id;
    private String guildID;
    private long groupID;
    private List<String> permissions;
}
