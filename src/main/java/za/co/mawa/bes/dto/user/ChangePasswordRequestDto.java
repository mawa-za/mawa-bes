package za.co.mawa.bes.dto.user;

/** Current and replacement credentials; identity is supplied only by the JWT principal. */
public class ChangePasswordRequestDto {
    private String oldPassword;
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
