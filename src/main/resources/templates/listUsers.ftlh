<#include "macros.ftlh">
<@htmlBase "List users" siteName>
<table id="usersList">
  <tr>
    <th>ID</th>
    <th>Username</th>
    <th>Email address</th>
    <th>Operator</th>
    <th>Created</th>
  </tr>
</table>
<div id="usersListViewOptions">
  <label for="usersListOrder">Order by</label>
  <select id="usersListOrder">
    <option value="username">Username</option>
    <option value="email">Email address</option>
    <option value="operator">Operator</option>
    <option value="when_created">When created</option>
  </select>
  <label for="usersListDesc">Descending order</label>
  <input type="checkbox" id="usersListDesc" value="true">
  <label for="usersListLimit">Limit</label>
  <input type="number" id="usersListLimit">
  <button onclick="updateUsersList(false)">Refresh</button>
</div>
<div id="pages"></div>
<script>
  updateUsersList(true);
</script>
</@htmlBase>