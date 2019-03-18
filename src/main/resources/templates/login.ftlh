<#include "macros.ftlh">
<@htmlBase "Login" siteName>
<form id="login" method="post">
  <fieldset>
    <legend>Login credentials</legend>
    <label for="query">Username / Email address</label>
    <input type="text" id="query" name="query"><br>
    <label for="password">Password</label>
    <input type="password" id="password" name="password"><br>
    <label for="remember">Remember me</label>
    <input type="checkbox" id="remember" name="remember" value="true">
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>