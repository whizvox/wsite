<#include "macros.ftlh">
<@htmlBase "Edit self" siteName>
<form id="editSelfUsername" method="post" action="/control/editSelfUsername">
  <fieldset>
    <legend>Edit username</legend>
    <label for="userNewUsername">New username</label>
    <input type="text" id="userNewUsername" name="username"><br>
    <input type="submit">
  </fieldset>
</form>
<form id="editSelfEmail" method="post" action="/control/editSelfEmail">
  <fieldset>
    <legend>Edit email address</legend>
    <label for="userNewEmail">New email address</label>
    <input type="email" id="userNewEmail" name="email"><br>
    <label for="userNewEmailConfirm">Confirm new email address</label>
    <input type="email" id="userNewEmailConfirm"><br>
    <input type="submit">
  </fieldset>
</form>
<form id="editSelfPassword" method="post" action="/control/editSelfPassword">
  <fieldset>
    <legend>Edit password</legend>
    <label for="userNewPassword">New password</label>
    <input type="password" id="userNewPassword" name="password"><br>
    <label for="userNewPasswordConfirm">Confirm new password</label>
    <input type="password" id="userNewPasswordConfirm"><br>
    <input type="submit">
  </fieldset>
</form>
</@htmlBase>