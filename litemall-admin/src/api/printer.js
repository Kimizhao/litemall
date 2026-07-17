import request from '@/utils/request'

export function listPrinter(query) {
  return request({
    url: '/printer/list',
    method: 'get',
    params: query
  })
}

export function createPrinter(data) {
  return request({
    url: '/printer/create',
    method: 'post',
    data
  })
}

export function updatePrinter(data) {
  return request({
    url: '/printer/update',
    method: 'post',
    data
  })
}

export function deletePrinter(data) {
  return request({
    url: '/printer/delete',
    method: 'post',
    data
  })
}

export function printTest(data) {
  return request({
    url: '/printer/print-test',
    method: 'post',
    data
  })
}

export function print(data) {
  return request({
    url: '/printer/print',
    method: 'post',
    data
  })
}

export function cleanQueue(data) {
  return request({
    url: '/printer/clean-queue',
    method: 'post',
    data
  })
}
