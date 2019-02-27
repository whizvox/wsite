<#include "macros.ftlh">
<@htmlBase "Login" siteName>
<form id="login" name="login" method="post">
  <fieldset>
    <legend>Login credentials</legend>
    <label for="username">Username / Email address</label>
    <input type="text" id="username" name="username"><br>
    <label for="password">Password</label>
    <input type="password" id="password" name="password"><br>
    <label for="remember_me">Remember me</label>
    <input type="checkbox" id="remember_me" name="remember_me" value="true">
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>