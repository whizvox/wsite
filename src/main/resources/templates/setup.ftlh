<#include "macros.ftlh">
<@htmlBase "Setup" "Wsite">
<form id="setup" name="setup" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>General</legend>
    <@siteConfigForm config />
  </fieldset>
  <fieldset>
    <legend>Database</legend>
    <@databaseConfigForm config />
  </fieldset>
  <fieldset>
    <legend>SSL</legend>
    <@sslConfigForm config />
  </fieldset>
  <fieldset>
    <legend>SMTP</legend>
    <@smtpConfigForm config />
  </fieldset>
  <fieldset>
    <legend>Initial user</legend>
    <@newUserForm true />
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>