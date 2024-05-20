<template>
  <v-table v-if="home.length >0" density="compact" class="text-center text-no-wrap">
    <thead>
      <tr>
        <th class="text-center font-weight-bold">Username</th>
        <th v-for="date in home[0].jsonUsage" :key="date.recorded" class="text-center font-weight-bold">
          {{date.recorded}}
        </th>
        <th class="text-center font-weight-bold">Change</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="user in home" :key="user.username">
        <td class="font-weight-bold">{{ user.username }}</td>
        <td v-for="date in user.jsonUsage" :key="date.recorded" v-bind:style="date.dirCol">
          {{ date.directories.toLocaleString() }}
        </td>
        <td v-if="user.dirsDifference < 0">
          <v-icon icon = "mdi-arrow-down-bold-circle" style="color: green"></v-icon> 
          {{ (user.dirsDifference*-1).toLocaleString() }}  
        </td>
        <td v-else-if="user.dirsDifference > 0">
          <v-icon icon = "mdi-arrow-up-bold-circle" style="color: red"></v-icon> 
          {{ user.dirsDifference.toLocaleString() }}  
        </td>
        <td v-else></td>
      </tr>
    </tbody>

  </v-table>
</template>

<script>
export default {
  name: 'Home Directories',
  props: ['home'],

  methods: {
    getNumberWithSuffix: function (value, k = 1024, separator = ' ') {
      if (value === undefined || value === null || value === 0) {
        return '0 '
      }
      const dm = 0
      const sizes = ['', 'K', 'M', 'G']
      var i = Math.floor(Math.log(value) / Math.log(k))
      if(i>3) {i=3}
      return parseFloat((value / Math.pow(k, i)).toFixed(dm)).toLocaleString() + separator + sizes[i]
    }
  }
}
</script>

<style>

</style>