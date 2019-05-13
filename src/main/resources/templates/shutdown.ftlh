<#include "macros.ftlh">
<@htmlBase "Shutdown confirmation" siteName>
<form id="shutdownConfirm" method="post">
  <p>Are you sure you want to shutdown the server?</p>
  <label for="confirm">Yes</label>
  <input type="checkbox" id="confirm" name="confirm" value="true"><br>
  <input type="submit">
</form>
</@htmlBase>