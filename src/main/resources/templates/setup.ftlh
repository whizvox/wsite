<#include "macros.ftlh">
<@htmlBase "Setup" siteName>
<form id="setup" method="post" enctype="multipart/form-data">
  <@configFormBase config />
  <fieldset>
    <legend>Initial operator</legend>
    <@userFormBase true />
  </fieldset>
  <input type="submit">
</form>
</@htmlBase>