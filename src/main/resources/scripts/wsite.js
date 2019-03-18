function $(id) {
  return document.getElementById(id);
}

function goto(url) {
  window.location.href = url;
}

function displayErrorMessage(msg) {
  var eErrorBox = $("errorBox");
  eErrorBox.hidden = false;

}

function useLoginToken(data) {
  data.append("useLoginToken", true);
}

function toggleSubmit(d) {
  var submitBtn = $("submitBtn");
  submitBtn.disabled = d;
}

function confirmUserEmail() {
  var eEmail = $("userEmailAddress");
  var eEmailConfirm = $("userConfirmEmailAddress");
  if (eEmail.value !== eEmailConfirm.value) {
    eEmailConfirm.validationMessage = "Email addresses must match";
    toggleSubmit(false);
  } else {
    eEmailConfirm.validationMessage = "";
    toggleSubmit(true);
  }
}

function confirmUserPassword() {
  var ePassword = $("userPassword");
  var ePasswordConfirm = $("userConfirmPassword");
  if (!ePassword.value !== ePasswordConfirm.value) {
    ePasswordConfirm.validationMessage = "Passwords must match";
    toggleSubmit(false);
  } else {
    ePasswordConfirm.validationMessage = "";
    toggleSubmit(true);
  }
}

function formCreateUser() {
  var eUsername = $("userUsername");
  var eEmail = $("userEmailAddress");
  var ePassword = $("userPassword");
  var eOperator = $("operator");
  createUser(eUsername.value, eEmail.value, ePassword.value, eOperator.value, function(obj) {
    if (obj.success) {
      goto("/register?success=true");
    } else {
      goto("/register?success=false&result=" + obj.message);
    }
    return false;
  });
}

function formDeleteUser() {
  var eQuery = $("userQuery");
}

function checkUsernameAvailable() {
  /*var eUsername = $("userUsername");
  */
}

function checkEmailAvailable() {
  /*var eEmail = $("userEmail");
  */
}

function handleAsyncJsonRequest(location, formData, callback) {
  var req = new XMLHttpRequest();
  req.open("post", "/api/users/search", true);
  req.onreadystatechange = function() {
    if (req.readyState === 4) {
      callback(JSON.parse(req.responseText));
    }
  };
  req.send(formData);
}

function searchUsers(query, callback) {
  var data = new FormData();
  useLoginToken(data);
  data.append("query", query);
  handleAsyncJsonRequest("/api/users/search", data, callback);
}

function getUser(id, callback) {
  var data = new FormData();
  useLoginToken(data);
  data.append("id", id);
  handleAsyncJsonRequest("/api/users/fetch", data, callback);
}

function createUser(username, email, password, operator, callback) {
  var data = new FormData();
  useLoginToken(data);
  data.append("username", username);
  data.append("email", email);
  data.append("password", password);
  data.append("operator", operator);
  handleAsyncJsonRequest("/api/users/create", data, callback);
}

function getPage(path, callback) {
  var data = new FormData();
  data.append("path", path);
  handleAsyncJsonRequest("/api/pages/fetch", data, callback);
}

function createPage(path, title, contents, syntax, callback) {
  var data = new FormData();
  data.append("path", path);
  data.append("title", title);
  data.append("contents", btoa(contents));
  data.append("syntax", syntax);
  data.append("encoded", "true");
  handleAsyncJsonRequest("/api/pages/create", data, callback);
}

function getConfig(filter, callback) {
  var data = new FormData();
  useLoginToken(data);
  data.append("filter", filter);
  handleAsyncJsonRequest("/api/config/fetch", data, callback);
}
