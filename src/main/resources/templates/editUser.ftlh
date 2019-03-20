<#include "macros.ftlh">
<@htmlBase "Edit user" siteName>
<form id="editUser" method="post">
  <label for="userId">User ID</label>
  <input type="text" id="userId" name="id"><br>
  <label for="userUsername">Username</label>
  <input type="text" id="userUsername" name="username"><br>
  <label for="userEmail">Email address</label>
  <input type="email" id="userEmail" name="email">
  <button onclick="updateUserInfo();return false;">Fill in</button><br>
  <label for="userPassword">Password</label>
  <input type="password" id="userPassword" name="password"><br>
  <label for="userOperator">Operator</label>
  <input type="checkbox" id="userOperator" name="operator" value="true"><br>
  <input type="submit">
</form>
</@htmlBase>
