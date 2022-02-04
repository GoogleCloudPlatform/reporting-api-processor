module github.com/GoogleCloudPlatform/reporting-api-processor/forwarder

go 1.17

require (
	github.com/labstack/echo/v4 v4.6.3
	github.com/rs/zerolog v1.26.1
	google.golang.org/protobuf v1.27.1
)

require (
	github.com/golang-jwt/jwt v3.2.2+incompatible // indirect
	github.com/labstack/gommon v0.3.1 // indirect
	github.com/mattn/go-colorable v0.1.11 // indirect
	github.com/mattn/go-isatty v0.0.14 // indirect
	github.com/valyala/bytebufferpool v1.0.0 // indirect
	github.com/valyala/fasttemplate v1.2.1 // indirect
	golang.org/x/crypto v0.0.0-20211215165025-cf75a172585e // indirect
	golang.org/x/net v0.0.0-20210913180222-943fd674d43e // indirect
	golang.org/x/sys v0.0.0-20211103235746-7861aae1554b // indirect
	golang.org/x/text v0.3.7 // indirect
	golang.org/x/time v0.0.0-20201208040808-7e3f01d25324 // indirect
)

replace github.com/GoogleCloudPlatform/reporting-api-processor/forwarder/proto => ./proto
