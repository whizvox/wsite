<#macro htmlBase title siteName>
<!DOCTYPE html>
<html lang="en">
  <head>
    <title>${title} // ${siteName}</title>
    <meta charset="UTF-8">
    <link rel="stylesheet" type="text/css" href="/assets/css/normalize.css">
    <link rel="stylesheet" type="text/css" href="/assets/css/main.css">
  </head>
  <main>
    <#nested>
  </main>
</html>
</#macro>

<#macro newUserForm doingSetup>
<label for="userUsername">Username</label>
<input type="text" id="userUsername" name="userUsername"><br>
<label for="userEmailAddress">Email address</label>
<input type="email" id="userEmailAddress" name="userEmailAddress"><br>
<label for="userConfirmEmailAddress">Confirm email address</label>
<input type="email" id="userConfirmEmailAddress" name="userConfirmEmailAddress"><br>
<label for="userPassword">Password</label>
<input type="password" id="userPassword" name="userPassword"><br>
<label for="userConfirmPassword">Confirm password</label>
<input type="password" id="userConfirmPassword" name="userConfirmPassword"><br>
<input type="checkbox" id="userOperator" name="userOperator" value="true" checked hidden><br>
<#if !doingSetup>
<label for="userOperator">Operator</label>
<input type="checkbox" id="userOperator" name="userOperator" value="true"><br>
</#if>
</#macro>

<#macro siteConfigForm config>
<label for="siteName">Site name</label>
<input type="text" id="siteName" name="siteName" value="${config.siteName}"><br>
<label for="port">Port number</label>
<input type="number" id="port" name="port" value="${config.port?c}"><br>
<label for="continuouslyRestart">Continuously restart</label>
<#if config.continuouslyRestart>
<input type="checkbox" id="continuouslyRestart" name="continuouslyRestart" value="true" checked><br>
<#else>
<input type="checkbox" id="continuouslyRestart" name="continuouslyRestart" value="true"><br>
</#if>
<label for="restartInterval">Restart interval</label>
<input type="number" id="restartInterval" name="restartInterval" placeholder="Minutes" value="${config.restartInterval?c}"><br>
<label for="usernamePattern">Username regex pattern</label>
<input type="text" id="usernamePattern" name="usernamePattern" placeholder="Regex" value="${config.usernamePattern}"><br>
<label for="passwordPattern">Password regex pattern</label>
<input type="text" id="passwordPattern" name="passwordPattern" placeholder="Regex" value="${config.passwordPattern}"><br>
</#macro>

<#macro databaseConfigForm config>
<label for="databaseUrl">JDBC Connection URL</label>
<input type="text" id="databaseUrl" name="databaseUrl" value="${config.databaseUrl}"><br>
<label for="databaseUsername">Username</label>
<input type="text" id="databaseUsername" name="databaseUsername" value="${config.databaseUsername!""}"><br>
<label for="databasePassword">Password</label>
<input type="password" id="databasePassword" name="databasePassword" value="${config.databasePassword!""}"><br>
<label for="databaseProperties">Properties</label>
<input type="text" id="databaseProperties" name="databaseProperties" placeholder="Separate with semicolons (;)" value="${config.properties!""}">
</#macro>

<#macro sslConfigForm config>
<label for="enableSsl">Enable SSL</label>
<#if config.enableSsl>
<input type="checkbox" id="enableSsl" name="enableSsl" value="true" checked><br>
<#else>
<input type="checkbox" id="enableSsl" name="enableSsl" value="true" checked><br>
</#if>
<label for="keystoreFile">Keystore file</label>
<input type="file" id="keystoreFile" name="keystoreFile"><br>
<label for="keystorePassword">Keystore password</label>
<input type="password" id="keystorePassword" name="keystorePassword" value="${config.keystorePassword!""}"><br>
<label for="truststoreFile">Truststore file</label>
<input type="file" id="truststoreFile" name="truststoreFile"><br>
<label for="truststorePassword">Truststore password</label>
<input type="password" id="truststorePassword" name="truststorePassword" value="${config.truststorePassword!""}">
</#macro>

<#macro smtpConfigForm config>
<label for="enableSmtp">Enable SMTP</label>
<#if config.enableSmtp>
<input type="checkbox" id="enableSmtp" name="enableSmtp" value="true" checked><br>
<#else>
<input type="checkbox" id="enableSmtp" name="enableSmtp" value="true"><br>
</#if>
<label for="smtpHost">Host</label>
<input type="text" id="smtpHost" name="smtpHost" value="${config.smtpHost!""}"><br>
<label for="smtpFrom">From email address</label>
<input type="text" id="smtpFrom" name="smtpFrom" value="${config.smtpFrom!""}"><br>
<label for="smtpUser">Username</label>
<input type="text" id="smtpUser" name="smtpUser" value="${config.smtpUser!""}"><br>
<label for="smtpPassword">Password</label>
<input type="password" id="smtpPassword" name="smtpPassword" value="${config.smtpPassword!""}">
</#macro>
