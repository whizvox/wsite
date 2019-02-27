<#include "macros.ftlh">
<@htmlBase "Create new user" siteName>
<script>
  var eEmailAddress = document.getElementById('email_address');
  var eConfirmEmailAddress = document.getElementById('confirm_email_address');
  var ePassword = document.getElementById('password');
  var eConfirmPassword = document.getElementById('confirm_password');
  var submitBtn = document.getElementById('submit');
  function validateEmailAddress() {
    /*if (eEmailAddress.value !== eConfirmEmailAddress.value) {
      eConfirmEmailAddress.setCustomValidity('Email addresses don\'t match.');
      submitBtn.disabled = true;
    } else {
      eConfirmEmailAddress.setCustomValidity('');
      submitBtn.disabled = false;
    }*/
  }
  function validatePassword() {
    /*if (ePassword.value !== eConfirmPassword.value) {
      eConfirmPassword.setCustomValidity('Passwords don\'t match.');
      submitBtn.disabled = true;
    } else {
      eConfirmPassword.setCustomValidity('');
      submitBtn.disabled = false;
    }*/
  }
</script>
<form id="newUser" name="newUser" method="post">
  <@userForm />
</form>
</@htmlBase>