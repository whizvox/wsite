<#include "macros.ftlh">
<@htmlBase "Edit self" siteName>
<form id="editSelf" method="post">
  <fieldset>
    <legend>Edit username</legend>
    <label for="userNewUsername">New username</label>
    <input type="text" id="userNewUsername" name="username"><br>
    <label for="userNewUsernameConfirm">Confirm new username</label>
    <input type="text" id="userNewUsernameConfirm"><br>
  </fieldset>
  <fieldset>
    <legend>Edit email address</legend>
    <label for="userNewEmail">New email address</label>
    <input type="email" id="userNewEmail" name="email"><br>
    <label for="userNewEmailConfirm">Confirm new email address</label>
    <input type="email" id="userNewEmailConfirm"><br>
  </fieldset>
  <fieldset>
    <legend>Edit password</legend>
    <label for="userNewPassword">New password</label>
    <input type="password" id="userNewPassword" name="password"><br>
    <label for="userNewPasswordConfirm">Confirm new password</label>
    <input type="password" id="userNewPasswordConfirm"><br>
  </fieldset>
  <label for="userCurrentPassword">Current password</label>
  <input type="password" id="userCurrentPassword" name="currentPassword"><br>
  <input type="submit">
</form>
</@htmlBase>