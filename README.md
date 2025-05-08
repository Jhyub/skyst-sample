# skyst-sample

Frontend 연습을 위한 샘플용 todo 백엔드입니다.

환경변수 `H2_PASSWORD`를 설정한 후 실행하는 것을 권장합니다.

## 구성

크게 User, UserToken, Task 세 가지 요소로 구성됩니다.

client는 User의 비밀번호를 입력해서 UserToken을 발급받을 수 있습니다.
인증이 필요한 모든 작업에는 Token이 요구됩니다.
Token은 64자리 sha256 문자열입니다.

client는 UserToken을 이용하여 User의 정보를 바꾸거나, Task를 만들거나 수정하거나 삭제할 수 있습니다.
모든 User는 자신의 Task만 확인할 수 있습니다.

## API
### 공통사항
모든 응답은 JSON으로 주어집니다.

모든 date는 `2025/05/09`형태의 String으로 주고 받습니다.

type은 편의상 Kotlin을 기준으로 적었으나 이해하는 데에 무리는 없을 것입니다.

#### Login

Login을 필요로 하는 API들은 Login 과정에서 다음 동작을 합니다.

Requires 
* `token`: `String`

Returns
* `400 Bad Request`, if `token` is not given.
* `404 Not Found`, if `token` is not found in the database.
* `403 Forbidden`, if the token was expired.
* `403 Forbidden`, if `user_id` was given as a path parameter (refer to `GET /users/{user_id}`), but it didn't match the token's owner's id.

### POST /users/create
Requires
* `name`: `String`
* `email`: `String`
* `password`: `String`

Returns
* `400 Bad Request`, if any of the requirements are not given.
* `400 Bad Request`, if a user with the given email already exists.
* `201 Created`, with
  * `id`: `Int` - id of the newly created user

### POST /users/login
Requires
* `email`: `String`
* `password`: `String`

Returns
* `400 Bad Request`, if any of the requirements are not given.
* `404 Not Found`, if a user with the given email doesn't exist.
* `403 Forbidden`, if the given password doesn't match.
* `200 OK`, with
  * `token`: `String` - the newly generated token

### GET /users/{user_id}
\<Login Required\>

Returns `200 OK`, with
* `id`: `Int` - id of the user
* `name`: `String` - trivial
* `email`: `String` - trivial

### POST /users/{user_id}/update
\<Login Required\>

Requires
* `name`: `String` *not mandatory
* `password`: `String` *not mandatory

Returns `200 OK`

All tokens will be expired when the password is changed.

### GET /users/tokens/{token}
Returns
* `404 Not Found`, if the given token doesn't exist.
* `200 OK`, with
  * `owner`: `Int` - id of the user that owns the token.
  * `is_expired`: `Boolean` - trivial

### POST /users/tokens/{token}/expire
Returns
* `404 Not Found`, if the given token doesn't exist.
* `200 OK`

The token will be expired and not function any more when you call this.
Use this to implement log out.

### GET /tasks
\<Login Required\>

Requires
* `date`: `String` *not mandatory
* `no_detail`: `Boolean` *not mandatory

Returns `200 OK`, with `tasks`, a object that includes all tasks owned by the user on default. 
If `date` is given, only the tasks whose due date is the given `date` will be included.
If `no_detail` is explicitly set to `true`, each task will only show its task id. 
Else, every detail will be shown. The details provided are the same as `GET /tasks/{id}`.

### POST /tasks/create
\<Login Required\>

Requires
* `name`: `String`
* `description`: `String` *not mandatory
* `due_date`: `String`

Returns
* `400 Bad Request`, if any of the mandatory requirements are not given.
* `201 Created`, with
  * `id`: `Int` - id of the newly created task

All newly created tasks are not completed by default.

### GET /tasks/{id}
\<Login Required\>

Returns
* `404 Not Found`, if a task with the given id doesn't exist.
* `403 Forbidden`, if the task's owner doesn't match the token's owner.
* `200 OK`, with
  * `id`: `Int` - id of the task
  * `name`: `String` - trivial
  * `description`: `String` - *might not be given if it was not set
  * `due_date`: `String` - trivial
  * `is_completed`: `Boolean` - trivial

### POST /tasks/{id}/update
\<Login Required\>

Requires
* `name`: `String` *not mandatory
* `description`: `String` *not mandatory
* `due_date`: `String` *not mandatory
* `is_completed`: `Boolean` *not mandatory

Returns
* `404 Not Found`, if a task with the given id doesn't exist.
* `403 Forbidden`, if the task's owner doesn't match the token's owner.
* `200 OK`

### POST /tasks/{id}/delete
\<Login Required\>

Returns
* `404 Not Found`, if a task with the given id doesn't exist.
* `403 Forbidden`, if the task's owner doesn't match the token's owner.
* `200 OK`
