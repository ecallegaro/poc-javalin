package org.example

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.Javalin
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.ServiceUnavailableResponse
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info

fun main(args: Array<String>) {
    val userDao = UserDao()

    val app = Javalin.create{

        fun getConfiguredOpenApiPlugin() = OpenApiPlugin(
            OpenApiOptions(
                Info().apply {
                    version("1.0")
                    description("User API")
                }
            ).apply {
                path("/swagger-docs") // endpoint for OpenAPI json
                swagger(SwaggerOptions("/swagger-ui")) // endpoint for swagger-ui
                reDoc(ReDocOptions("/redoc")) // endpoint for redoc
                defaultDocumentation { doc ->
                    doc.json("500", InternalServerErrorResponse::class.java)
                    doc.json("503", ServiceUnavailableResponse::class.java)
                }
            }
        )

        it.registerPlugin(getConfiguredOpenApiPlugin())
        it.defaultContentType = "application/json"

    }.
    apply {
        exception(Exception::class.java) { e, ctx -> e.printStackTrace() }
        error(404) { ctx -> ctx.json("not found") }
    }.start(7000)

    println("Check out ReDoc docs at http://localhost:7000/redoc")
    println("Check out Swagger UI docs at http://localhost:7000/swagger-ui")

    app.routes {

        get("/users") { ctx ->
            ctx.json(userDao.users)
        }

        get("/users/:user-id") { ctx ->
            ctx.json(userDao.findById(ctx.pathParam("user-id").toInt())!!)
        }

        get("/users/email/:email") { ctx ->
            ctx.json(userDao.findByEmail(ctx.pathParam("email"))!!)
        }

        post("/users") { ctx ->
            val user = ctx.body<User>()
            userDao.save(name = user.name, email = user.email)
            ctx.status(201)
        }

        patch("/users/:user-id") { ctx ->
            val user = ctx.body<User>()
            userDao.update(
                id = ctx.pathParam("user-id").toInt(),
                user = user
            )
            ctx.status(204)
        }

        delete("/users/:user-id") { ctx ->
            userDao.delete(ctx.pathParam("user-id").toInt())
            ctx.status(204)
        }
    }
}

