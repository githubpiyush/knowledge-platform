package org.sunbird.graph

import java.util
import org.sunbird.common.dto.{Property, Request, Response}
import org.sunbird.graph.dac.model.{Node, SearchCriteria}
import org.sunbird.graph.external.ExternalPropsManager
import org.sunbird.graph.external.store.ExternalStore
import org.sunbird.graph.service.operation.{GraphAsyncOperations, Neo4JBoltSearchOperations, NodeAsyncOperations, SearchAsyncOperations}

//import java.time.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class GraphService {
    implicit  val ec: ExecutionContext = ExecutionContext.global

    def addNode(graphId: String, node: Node): Future[Node] = {
        val result = NodeAsyncOperations.addNode(graphId, node)
        println("Inside addNode function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getMetadata,value.getObjectType,value.getExternalData,value.toString)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def upsertNode(graphId: String, node: Node, request: Request): Future[Node] = {
        val result = NodeAsyncOperations.upsertNode(graphId, node, request)
        println("Inside upsertNode function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getMetadata,value.getObjectType,value.getExternalData,value.getNode)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def upsertRootNode(graphId: String, request: Request): Future[Node] = {
        val result = NodeAsyncOperations.upsertRootNode(graphId, request)
        println("Inside upsertRootNode function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getMetadata,value.getObjectType,value.getExternalData,value.getNode)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def getNodeByUniqueId(graphId: String, nodeId: String, getTags: Boolean, request: Request): Future[Node] = {
        val result = SearchAsyncOperations.getNodeByUniqueId(graphId, nodeId, getTags, request)
        println("Inside getNodeByUniqueId function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getMetadata,value.getObjectType,value.getExternalData,value.getNode)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def deleteNode(graphId: String, nodeId: String, request: Request): Future[java.lang.Boolean] = {
        val result = NodeAsyncOperations.deleteNode(graphId, nodeId, request)
        println("Inside deleteNode function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def getNodeProperty(graphId: String, identifier: String, property: String): Future[Property] = {
        val result = SearchAsyncOperations.getNodeProperty(graphId, identifier, property)
        println("Inside getNodeProperty function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getPropertyName, value.getPropertyValue,value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }
    def updateNodes(graphId: String, identifiers:util.List[String], metadata:util.Map[String,AnyRef]):Future[util.Map[String, Node]] = {
        val result = NodeAsyncOperations.updateNodes(graphId, identifiers, metadata)
        println("Inside updateNodes function **********\n\n")
        result.onComplete ({
            case Success(value) => print(value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def getNodeByUniqueIds(graphId:String, searchCriteria: SearchCriteria): Future[util.List[Node]] = {
        val result = SearchAsyncOperations.getNodeByUniqueIds(graphId, searchCriteria)
        println("Inside getNodeByUniqueIds function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(2000)

        result
    }

    def readExternalProps(request: Request, fields: List[String]): Future[Response] = {
        val result = ExternalPropsManager.fetchProps(request, fields)
        println("Inside readExternalProps function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getResult,value.getParams,value.toString)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def saveExternalProps(request: Request): Future[Response] = {
        val result =  ExternalPropsManager.saveProps(request)
        println("Inside saveExternalProps function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getResult, value.getParams,value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def updateExternalProps(request: Request): Future[Response] = {
        val result = ExternalPropsManager.update(request)
        println("Inside updateExternalProps function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getResult, value.getParams,value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def deleteExternalProps(request: Request): Future[Response] = {
        val result = ExternalPropsManager.deleteProps(request)
        println("Inside deleteExternalProps function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getResult, value.getParams,value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }
    def checkCyclicLoop(graphId:String, endNodeId: String, startNodeId: String, relationType: String) = {
        Neo4JBoltSearchOperations.checkCyclicLoop(graphId, endNodeId, relationType, startNodeId)
    }

    def removeRelation(graphId: String, relationMap: util.List[util.Map[String, AnyRef]]) = {
        val result = GraphAsyncOperations.removeRelation(graphId, relationMap)
        println("Inside removeRelation function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getResult, value.getParams,value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }

    def createRelation(graphId: String, relationMap: util.List[util.Map[String, AnyRef]]) = {
        val result = GraphAsyncOperations.createRelation(graphId, relationMap)
        println("Inside createRelation function **********\n\n")
        result.onComplete ({
            case Success(value) => println(value.getResult, value.getParams,value)
            case Failure(ex) => println(ex.getMessage)
        })
        Thread.sleep(1000)
        result
    }
}

