<#include "macros.ftlh">
<@htmlBase "Delete user" siteName>
<form id="deleteUser" method="post">
  <label for="id">ID</label>
  <input type="text" id="id" name="id"><br>
  <label for="username">Username</label>
  <input type="text" id="username" name="username"><br>
  <label for="email_address">Email address</label>
  <input type="email" id="email_address" name="email_address"><br>
  <input type="submit">
</form>
</@htmlBase>