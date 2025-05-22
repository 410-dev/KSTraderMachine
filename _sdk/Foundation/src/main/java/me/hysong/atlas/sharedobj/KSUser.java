package me.hysong.atlas.sharedobj;

import lombok.Getter;
import me.hysong.atlas.utils.SIDKit;

@Getter
public class KSUser {

    public static final String DefaultUserName = "default-user";
    public static final String DefaultUserDisplayName = "Default User";

    private final String SID;
    private final String username;
    private final String userDisplayName;
    private final Registry registry;

    public KSUser() {
        this.SID = SIDKit.generateSID(SIDKit.SIDType.USER_OBJECT);
        this.username = DefaultUserName;
        this.userDisplayName = DefaultUserDisplayName;
        this.registry = new Registry();
    }
}
