<#macro htmlBase title siteName>
<!DOCTYPE html>
<html lang="en">
  <head>
    <title>${title} // ${siteName}</title>
    <meta charset="UTF-8">
    <link rel="stylesheet" type="text/css" href="/assets/css/normalize.css">
    <link rel="stylesheet" type="text/css" href="/assets/css/main.css">
    <script src="/assets/scripts/cookies.min.js"></script>
    <script src="/assets/scripts/wsite.js"></script>
  </head>
  <main>
    <#nested>
  </main>
</html>
</#macro>

<#macro userFormBase doingSetup>
<label for="userUsername">Username</label>
<input type="text" id="userUsername" name="username" onchange="checkUsernameAvailable()"><br>
<label for="userEmail">Email address</label>
<input type="email" id="userEmail" name="email" onchange="confirmUserEmail(); checkEmailAvailable()"><br>
<label for="userConfirmEmailAddress">Confirm email address</label>
<input type="email" id="userConfirmEmailAddress" name="confirmEmail" onchange="confirmUserEmail()"><br>
<label for="userPassword">Password</label>
<input type="password" id="userPassword" name="password" onchange="confirmUserPassword()"><br>
<label for="userConfirmPassword">Confirm password</label>
<input type="password" id="userConfirmPassword" name="confirmPassword" onchange="confirmUserPassword()"><br>
<#if !doingSetup>
<label for="userOperator">Operator</label>
<input type="checkbox" id="userOperator" name="operator" value="true"><br>
<#else>
<input type="checkbox" id="userOperator" name="operator" value="true" checked hidden>
</#if>
</#macro>

<#macro pageFormBase formName>
<label for="path">Path</label>
<input id="path" name="path"><br>
<label for="title">Title</label>
<input id="title" name="title"><br>
<label for="syntax">Syntax</label>
<select form="editPage" id="syntax" name="syntax">
  <option value="markdown">Markdown</option>
  <option value="embedded">Embedded Plain Text</option>
  <option value="html_limited_embedded">Embedded Limited HTML</option>
  <option value="html_embedded">Embedded Raw HTML</option>
  <option value="plain">Plain Text</option>
  <option value="html_limited">Limited HTML</option>
  <option value="html">Raw HTML</option>
</select><br>
<label for="contents">Contents</label>
<textarea form="${formName}" id="contents" name="contents" rows="50" cols="100"></textarea><br>
</#macro>

<#macro siteConfigFormBase config>
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
<label for="indexPage">Index page</label>
<input type="text" id="indexPage" name="indexPage" value="${config.indexPage}"><br>
</#macro>

<#macro databaseConfigFormBase config>
<label for="databaseUrl">JDBC Connection URL</label>
<input type="text" id="databaseUrl" name="databaseUrl" value="${config.databaseUrl}"><br>
<label for="databaseUsername">Username</label>
<input type="text" id="databaseUsername" name="databaseUsername" value="${config.databaseUsername!""}"><br>
<label for="databasePassword">Password</label>
<input type="password" id="databasePassword" name="databasePassword" value="${config.databasePassword!""}"><br>
<label for="databaseProperties">Properties</label>
<input type="text" id="databaseProperties" name="databaseProperties" placeholder="Separate with semicolons (;)" value="${config.properties!""}">
</#macro>

<#macro sslConfigFormBase config>
<label for="enableSsl">Enable SSL</label>
<#if config.enableSsl>
<input type="checkbox" id="enableSsl" name="enableSsl" value="true" checked><br>
<#else>
<input type="checkbox" id="enableSsl" name="enableSsl" value="true"><br>
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

<#macro smtpConfigFormBase config>
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
