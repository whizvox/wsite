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

<#macro userForm>
<label for="username">Username</label>
<input type="text" id="username" name="username"><br>
<label for="email_address">Email address</label>
<input type="email" id="email_address" name="email_address" onchange="validateEmailAddress()"><br>
<label for="confirm_email_address">Confirm email address</label>
<input type="email" id="confirm_email_address" name="confirm_email_address" onkeyup="validateEmailAddress()"><br>
<label for="password">Password</label>
<input type="password" id="password" name="password" onchange="validatePassword()"><br>
<label for="confirm_password">Confirm password</label>
<input type="password" id="confirm_password" name="confirm_password" onkeyup="validatePassword()"><br>
<label for="operator">Operator</label>
<input type="checkbox" id="operator" name="operator" value="true"><br>
<input type="submit" id="submit">
</#macro>