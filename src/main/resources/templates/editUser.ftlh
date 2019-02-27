<#include "macros.ftlh">
<@htmlBase "Edit user" siteName>
<script>
  function fillInUserDetails() {

  }
</script>
<form id="editUser" name="editUser" method="post">
  <@userForm />
  <button onclick="fillInUserDetails()">Update</button>
</form>
</@htmlBase>