package me.hysong.atlas.sharedobj;

import lombok.Getter;
import me.hysong.atlas.utils.SIDKit;

@Getter
public class KSUser {

    public static final String DefaultUserName = "default-user";
    public static final String DefaultUserDisplayName = "Default User";

    private String SID;
    private String username;
    private String userDisplayName;
    private Registry registry;

    public KSUser() {
        this.SID = SIDKit.generateSID(SIDKit.SIDType.USER_OBJECT);
        this.username = DefaultUserName;
        this.userDisplayName = DefaultUserDisplayName;
        this.registry = new Registry();
    }
}
