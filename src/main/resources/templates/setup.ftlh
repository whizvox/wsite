<#include "macros.ftlh">
<@htmlBase "Setup" "Wsite">
<form id="setup" name="setup" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>General</legend>
    <label for="siteName">Site name</label>
    <input type="text" id="siteName" name="siteName"><br>
    <label for="port">Port number</label>
    <input type="number" id="port" name="port"><br>
    <label for="rootDirectory">Root directory</label>
    <input type="text" id="rootDirectory" name="rootDirectory"><br>
    <label for="continuouslyRestart">Continuously restart</label>
    <input type="checkbox" id="continuouslyRestart" name="continuouslyRestart" value="true" checked><br>
    <label for="restartInterval">Restart interval</label>
    <input type="number" id="restartInterval" name="restartInterval" placeholder="Minutes"><br>
    <label for="usernamePattern">Username regex pattern</label>
    <input type="text" id="usernamePattern" name="usernamePattern" placeholder="Regex"><br>
    <label for="passwordPattern">Password regex pattern</label>
    <input type="text" id="passwordPattern" name="passwordPattern" placeholder="Regex"><br>
  </fieldset>
  <fieldset>
    <legend>Database</legend>
    <label for="dbUsername">Username</label>
    <input type="text" id="dbUsername" name="dbUsername"><br>
    <label for="dbPassword">Password</label>
    <input type="password" id="dbPassword" name="dbPassword"><br>
    <label for="dbProperties">Properties</label>
    <input type="text" id="dbProperties" name="dbProperties" placeholder="Separate with semicolons (;)">
  </fieldset>
  <fieldset>
    <legend>SSL</legend>
    <label for="enableSsl">Enable SSL</label>
    <input type="checkbox" id="enableSsl" name="enableSsl" value="true"><br>
    <label for="keystoreFile">Keystore file</label>
    <input type="file" id="keystoreFile" name="keystoreFile"><br>
    <label for="keystorePassword">Keystore password</label>
    <input type="password" id="keystorePassword" name="keystorePassword"><br>
    <label for="truststoreFile">Truststore file</label>
    <input type="file" id="truststoreFile" name="truststoreFile"><br>
    <label for="truststorePassword">Truststore password</label>
    <input type="password" id="truststorePassword" name="truststorePassword">
  </fieldset>
  <fieldset>
    <legend>SMTP</legend>
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
    <legend>Initial user</legend>
    <label for="iUsername">Username</label>
    <input type="text" id="iUsername" name="iUsername"><br>
    <label for="iEmailAddress">Email address</label>
    <input type="email" id="iEmailAddress" name="iEmailAddress"><br>
    <label for="iPassword">Password</label>
    <input type="password" id="iPassword" name="iPassword">
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>