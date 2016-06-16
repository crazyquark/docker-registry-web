package docker.registry.web

import docker.registry.Event
import docker.registry.Role
import docker.registry.User
import docker.registry.UserRole
import grails.transaction.Transactional

@Transactional
class UserController {

  def springSecurityService

  def index() {
    def users = User.list()
    [list: users]
  }

  def show() {
    def user = User.get(params.id)
    def roles = Role.list() - user.authorities
    def events = Event.findAllByUsername(user.username, [max: 10, sort: 'id', order: 'desc'])
    [user: user, roles: roles, events: events, current: user == springSecurityService.currentUser]
  }

  def edit() {
    def user = User.get(params.id)
    if (user == springSecurityService.currentUser)
      redirect(controller: 'account', action: 'index')
    [user: user]
  }

  def addRole() {
    def user = User.get(params.userId)
    def role = Role.get(params.roleId)
    new UserRole(user: user, role: role).save(failOnError: true)
    redirect(action: 'show', id: params.userId)
  }

  def deleteRole() {
    def user = User.get(params.userId)
    def role = Role.get(params.id)
    def userRole = UserRole.findByUserAndRole(user, role)
    //todo: prevent deletion of last admin
    userRole.delete()
    redirect(action: 'show', id: params.userId)
  }

  def update() {
    def user = User.get(params.id)
    user.accountLocked = params.accountLocked as boolean
    //update password only if it entered
    if (params.password) {
      user.password = params.password
    }

    if (user.isDirty()) {
      flash.message = "User '${user.username}' have been updated"
    }
    redirect action: 'show', id: params.id
  }

  def delete() {
    def user = User.get(params.id)
    if (user == springSecurityService.currentUser) {
      log.error("Can't delete current user!")
      redirect action: 'show', id: params.id
    } else {
      log.info "Deleting user: ${user}"
      UserRole.findAllByUser(user)*.delete()
      user.delete()
      redirect action: 'index'
    }
  }

  def add() {
  }

  def create() {
    def user = new User(params)
    user.password = params.list('password')[0]
    log.info "Creating user: ${user.username}"
    user.save()
    if (user.hasErrors()) {
      log.warn "Error saving user: ${user.errors}"
      flash.errors = user.errors
      redirect action: 'add'
    } else
    redirect action: 'show', id: user.id
  }
}
