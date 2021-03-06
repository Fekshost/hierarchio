package org.hierarchio

import com.google.gson.JsonObject
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.io.Closeable

class HiearchioServer(private val port: Int, private val repository: HierarchyRepository, loggerFactory: ILoggerFactory) : Closeable {
    private val logger: Logger = loggerFactory.getLogger(HiearchioServer::class.java.name)
    private var app: Javalin? = null

    fun start(){
        this.app = Javalin.create().apply {
            exception(Exception::class.java) { e, ctx -> e.printStackTrace() }
            error(404) { ctx -> ctx.json("not found") }
        }

        this.app!!.routes {
            ApiBuilder.get("/hierarchy") { ctx ->
                var hierarchy = repository.get()
                if (hierarchy != null) {
                    if (hierarchy.jsonReports == null || hierarchy.jsonReports.entrySet().isEmpty()) {
                        GsonEmployeeHierarchyResponseBuilder().build(hierarchy)
                    }
                    logger.info("Successfully retrieved employee hierarchy")
                    ctx.json(hierarchy.jsonReports.toString())
                } else {
                    logger.info("Failed ot get hierarchy, hierarchy was null")
                    ctx.json("no employees")
                }
            }

            ApiBuilder.get("/hierarchy/{name}") { ctx ->
                var sanitized = ctx.pathParam("name").replace("\"", "")
                var employee = repository.get(sanitized)
                if (employee != null) {
                    var jsonToReturn = StringBuilder()
                    EmployeeSupervisorResponseBuilder().use {
                        it.build(employee, jsonToReturn, 2)
                    }
                    ctx.json(jsonToReturn.toString())
                    ctx.status(200)
                }
                else{
                    logger.info("Failed to find employee with name ${ctx.pathParam("name")}")
                    ctx.status(404)
                }
            }

            ApiBuilder.post("/hierarchy") { ctx ->
                try {
                    val rawHierarchy = ctx.body()
                    var sanitized = rawHierarchy.replace("\"", "").replace("\\s".toRegex(), "")
                    repository.create(sanitized)
                    logger.info("Successfully created employee tree with provided client input.")
                    ctx.status(201)
                } catch (e:java.lang.Exception) {
                    when(e) {
                        is LoopDetectionException, is ParsingException, is MultipleRootNodeException -> {
                            logger.error(e.message)
                            ctx.status(400)
                            ctx.json(e.message.toString())
                        }
                    }
                }
            }
        }
        this.app!!.start(port)
        logger.info("Server successfully started on port $port")
    }


    override fun close() {
        app!!.stop()
        app!!.close()
        logger.info("Server disposed!")
    }
}