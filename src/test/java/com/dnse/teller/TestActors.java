package com.dnse.teller;

import com.dnse.teller.security.AuthenticatedActor;

/** Danh tính dùng chung cho test. */
public final class TestActors {

    public static final AuthenticatedActor TELLER =
        new AuthenticatedActor("GDV001", "Nguyễn Thị Hà", AuthenticatedActor.Role.TELLER, "CN-SGD-01");

    public static final AuthenticatedActor OTHER_TELLER =
        new AuthenticatedActor("GDV002", "Trần Văn Bình", AuthenticatedActor.Role.TELLER, "CN-SGD-01");

    public static final AuthenticatedActor SUPERVISOR =
        new AuthenticatedActor("KSV001", "Lê Minh Tú", AuthenticatedActor.Role.SUPERVISOR, "CN-SGD-01");

    /** Kiểm soát viên trùng userId với GDV — dùng để test vi phạm 4 mắt. */
    public static final AuthenticatedActor SUPERVISOR_SAME_PERSON =
        new AuthenticatedActor("GDV001", "Nguyễn Thị Hà", AuthenticatedActor.Role.SUPERVISOR, "CN-SGD-01");

    public static final AuthenticatedActor SECURITY_ADMIN =
        new AuthenticatedActor("SEC001", "Admin Bảo mật", AuthenticatedActor.Role.SECURITY_ADMIN, null);

    private TestActors() {}
}
