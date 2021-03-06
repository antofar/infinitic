<!--
 "Commons Clause" License Condition v1.0

 The Software is provided to you by the Licensor under the License, as defined
 below, subject to the following condition.

 Without limiting other conditions in the License, the grant of rights under the
 License will not include, and the License does not grant to you, the right to
 Sell the Software.

 For purposes of the foregoing, “Sell” means practicing any or all of the rights
 granted to you under the License to provide to third parties, for a fee or
 other consideration (including without limitation fees for hosting or
 consulting/ support services related to the Software), a product or service
 whose value derives, entirely or substantially, from the functionality of the
 Software. Any license notice or attribution required by the License must also
 include this Commons Clause License Condition notice.

 Software: Infinitic

 License: MIT License (https://opensource.org/licenses/MIT)

 Licensor: infinitic.io
-->

<template>
  <sidebar-with-header-layout>
    <template v-slot:header>
      <div class="flex-1 px-4 flex justify-between">
        <div class="flex-1 flex">
          <div class="w-full flex md:ml-0">
            <label for="search_field" class="sr-only">Search </label>
            <div
              class="relative w-full text-gray-400 focus-within:text-gray-600"
            >
              <div
                class="absolute inset-y-0 left-0 flex items-center pointer-events-none"
              >
                <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                  <path
                    fill-rule="evenodd"
                    clip-rule="evenodd"
                    d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                  />
                </svg>
              </div>
              <input
                id="search_field"
                class="block w-full h-full pl-8 pr-3 py-2 rounded-md text-gray-900 placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 sm:text-sm"
                placeholder="Search"
                type="search"
                v-model="searchInput"
                @keypress.enter="searchTask()"
              />
            </div>
          </div>
        </div>
      </div>
    </template>

    <main
      class="flex-1 relative z-0 overflow-y-auto pt-2 pb-6 focus:outline-none md:py-6"
      tabindex="0"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
        <pulse-loader
          v-if="loading"
          class="mt-5 text-center"
          color="#6875F5"
          :loading="true"
        />
        <div v-else-if="error">
          <server-error />
        </div>
        <div v-else>
          <task-metrics-list :taskTypes="taskTypes" />
        </div>
      </div>
    </main>
  </sidebar-with-header-layout>
</template>

<script lang="ts">
import Vue from "vue";
import { getTaskTypes, TaskType } from "@/api";
import SidebarWithHeaderLayout from "@/components/layouts/SidebarWithHeaderLayout.vue";
import TaskMetricsList from "@/components/tasks/TaskMetricsList.vue";
import ServerError from "@/components/errors/ServerError.vue";
import { PulseLoader } from "@saeris/vue-spinners";

export default Vue.extend({
  name: "Tasks",

  components: {
    SidebarWithHeaderLayout,
    PulseLoader,
    TaskMetricsList,
    ServerError
  },

  data() {
    return {
      searchInput: "",
      loading: false,
      taskTypes: undefined,
      error: undefined
    } as {
      searchInput: string;
      loading: boolean;
      taskTypes: TaskType[] | undefined;
      error: Error | undefined;
    };
  },

  created() {
    this.loadData();
  },

  methods: {
    async loadData() {
      if (this.loading) {
        return;
      }

      this.loading = true;

      try {
        this.taskTypes = await getTaskTypes();
      } catch (err) {
        this.error = err;
      } finally {
        this.loading = false;
      }
    },

    async searchTask() {
      if (this.searchInput.length > 0) {
        this.$router.push({
          name: "TaskDetails",
          params: { id: this.searchInput }
        });
      }
    }
  }
});
</script>
