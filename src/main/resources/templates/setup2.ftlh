<#include "macros.ftlh">
<@htmlBase "Setup" siteName>
<form id="setup" name="setup" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>Server Configuration</legend>
    <label for="siteName"></label>
    <input type="text" id="siteName" name="siteName"><br>
    <label for="port">Port</label>
    <input type="number" id="port" name="port" placeholder="1-65535"><br>
    <label for="restartInterval">Restart interval</label>
    <input type="number" id="restartInterval" name="restartInterval" placeholder="Minutes">
  </fieldset>
  <fieldset>
    <legend>SSL Configuration</legend>
    <label for="enableSsl">Enable SSL</label>
    <input type="checkbox" id="enableSsl" name="enableSsl" value="true">
    <label for="keystoreFile">Keystore file</label>
    <input type="file" id="keystoreFile" name="keystoreFile">
    <label for="keystorePassword">Keystore password</label>
    <input type="password" id="keystorePassword" name="keystorePassword">
    <label for="truststoreFile">Truststore file</label>
    <input type="file" id="truststoreFile" name="truststoreFile">
    <label for="truststorePassword">Truststore password</label>
    <input type="password" id="truststorePassword" name="truststorePassword">
  </fieldset>
  <fieldset>
    <legend>SMTP Configuration</legend>
    <label for="enableSmtp">Enable SMTP</label>
    <input type="checkbox" id="enableSmtp" name="enableSmtp" value="true"><br>
    <label for="smtpHost">Host</label>
    <input type="text" id="smtpHost" name="smtpHost"><br>
    <label for="smtpFrom">From email address</label>
    <input type="text" id="smtpFrom" name="smtpFrom"><br>
    <label for="smtpUser">Username</label>
    <input type="text" id="smtpUser" name="smtpUser"><br>
    <label for="smtpPassword">Password</label>
    <input type="password" id="smtpPassword" name="smtpPassword">
  </fieldset>
  <fieldset>
    <legend>Initial User Creation</legend>
    <@userForm />
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>