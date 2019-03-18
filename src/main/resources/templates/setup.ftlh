<#include "macros.ftlh">
<@htmlBase "Setup" "Wsite">
<form id="setup" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>General</legend>
    <@siteConfigFormBase config />
  </fieldset>
  <fieldset>
    <legend>Database</legend>
    <@databaseConfigFormBase config />
  </fieldset>
  <fieldset>
    <legend>SSL</legend>
    <@sslConfigFormBase config />
  </fieldset>
  <fieldset>
    <legend>SMTP</legend>
    <@smtpConfigFormBase config />
  </fieldset>
  <fieldset>
    <legend>Initial operator</legend>
    <@userFormBase true />
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>